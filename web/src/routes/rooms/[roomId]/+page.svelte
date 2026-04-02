<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { getMessages, getRoom, getUploadUrl, getDownloadUrl, leaveRoom, deleteRoom, searchMessages, getReadStatus, getRoomMembers, editMessage, deleteMessage, addReaction, removeReaction, type Message, type Room, type ReactionGroup } from '$lib/api';
	import { getAuthState } from '$lib/stores/auth.svelte';
	import { untrack } from 'svelte';
	import { subscribe, send, getWsState } from '$lib/websocket.svelte';
	import { setCurrentRoom, clearUnread } from '$lib/stores/notifications.svelte';
	import { HugeiconsIcon } from '@hugeicons/svelte';
	import { Attachment01Icon, Image01Icon, SentIcon } from '@hugeicons/core-free-icons';

	let room = $state<Room | null>(null);
	let messages = $state<Message[]>([]);
	let input = $state('');
	let messagesEnd: HTMLDivElement;
	let fileInput: HTMLInputElement;
	let showSearch = $state(false);
	let searchQuery = $state('');
	let searchResults = $state<Message[]>([]);
	let searching = $state(false);
	let imageUrls = $state<Record<string, string>>({});
	let currentPage = 0;
	let totalPages = 1;
	let loadingMore = $state(false);
	let messagesContainer: HTMLDivElement;
	let typingUsers = $state<Record<string, string>>({});
	let readStatus = $state<Record<string, string>>({});
	let reactions = $state<Record<string, ReactionGroup[]>>({});
	const QUICK_EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '🎉'];
	let editingId = $state<string | null>(null);
	let editContent = $state('');
	let showMentionSuggest = $state(false);
	let mentionQuery = $state('');
	let roomMembers = $state<{userId: string, userName: string}[]>([]);
	let typingTimeout: ReturnType<typeof setTimeout> | null = null;
	let lastTypingSent = 0;

	const roomId = $derived(page.params.roomId);
	const auth = getAuthState();
	const ws = getWsState();

	async function loadRoom() {
		try {
			room = await getRoom(roomId);
			currentPage = 0;
			const res = await getMessages(roomId, 0);
			totalPages = res.totalPages;
			messages = res.content.reverse();
			messages.forEach(resolveImageUrl);
			scrollToBottom();
			try { readStatus = await getReadStatus(roomId); } catch {}
			try { roomMembers = await getRoomMembers(roomId); } catch {}
			if (messages.length > 0) {
				send(`/app/read/${roomId}`, { messageId: messages[messages.length - 1].id });
			}
		} catch {
			goto('/rooms');
		}
	}

	async function loadMoreMessages() {
		if (loadingMore || currentPage + 1 >= totalPages) return;
		loadingMore = true;
		try {
			const prevScrollHeight = messagesContainer?.scrollHeight || 0;
			const res = await getMessages(roomId, currentPage + 1);
			currentPage++;
			const older = res.content.reverse();
			older.forEach(resolveImageUrl);
			messages = [...older, ...messages];
			// Maintain scroll position
			setTimeout(() => {
				if (messagesContainer) {
					messagesContainer.scrollTop = messagesContainer.scrollHeight - prevScrollHeight;
				}
			}, 0);
		} catch {
			// ignore
		} finally {
			loadingMore = false;
		}
	}

	function handleScroll() {
		if (messagesContainer && messagesContainer.scrollTop === 0) {
			loadMoreMessages();
		}
	}

	function scrollToBottom() {
		setTimeout(() => messagesEnd?.scrollIntoView({ behavior: 'smooth' }), 50);
	}

	function subscribeToRoom() {
		const sub = subscribe(`/topic/room.${roomId}`, (msg) => {
			const message: Message = JSON.parse(msg.body);
			messages = [...messages, message];
			resolveImageUrl(message);
			scrollToBottom();
			send(`/app/read/${roomId}`, { messageId: message.id });
		});
		const readSub = subscribe(`/topic/room.${roomId}.read`, (msg) => {
			const data = JSON.parse(msg.body);
			readStatus = { ...readStatus, [data.userId]: data.messageId };
		});
		const reactionSub = subscribe(`/topic/room.${roomId}.reactions`, (msg) => {
			const data = JSON.parse(msg.body);
			reactions = { ...reactions, [data.messageId]: data.reactions };
		});
		const updateSub = subscribe(`/topic/room.${roomId}.update`, (msg) => {
			const updated: Message = JSON.parse(msg.body);
			messages = messages.map((m) => (m.id === updated.id ? updated : m));
		});
		const deleteSub = subscribe(`/topic/room.${roomId}.delete`, (msg) => {
			const { messageId } = JSON.parse(msg.body);
			messages = messages.filter((m) => m.id !== messageId);
		});
		return { unsubscribe: () => { sub?.unsubscribe(); readSub?.unsubscribe(); reactionSub?.unsubscribe(); updateSub?.unsubscribe(); deleteSub?.unsubscribe(); } };
	}

	async function handleEdit(msg: Message) { editingId = msg.id; editContent = msg.content; }

	async function handleEditSave() {
		if (!editingId || !editContent.trim()) return;
		try { await editMessage(editingId, editContent); editingId = null; editContent = ''; } catch {}
	}

	async function handleDeleteMessage(msgId: string) {
		if (!confirm('このメッセージを削除しますか？')) return;
		try { await deleteMessage(msgId); } catch {}
	}

	async function toggleReaction(msgId: string, emoji: string) {
		const msgReactions = reactions[msgId] || [];
		const existing = msgReactions.find((r) => r.emoji === emoji && r.userIds.includes(auth.user?.sub || ''));
		try {
			if (existing) {
				await removeReaction(msgId, emoji);
			} else {
				await addReaction(msgId, emoji);
			}
		} catch { /* ignore */ }
	}

	function subscribeToTyping() {
		return subscribe(`/topic/room.${roomId}.typing`, (msg) => {
			const data = JSON.parse(msg.body);
			if (data.userId === auth.user?.sub) return;
			typingUsers = { ...typingUsers, [data.userId]: data.userName };
			setTimeout(() => {
				const { [data.userId]: _, ...rest } = typingUsers;
				typingUsers = rest;
			}, 3000);
		});
	}

	function sendTyping() {
		const now = Date.now();
		if (now - lastTypingSent < 2000) return;
		lastTypingSent = now;
		send(`/app/typing/${roomId}`, {});
	}

	function handleSend() {
		if (!input.trim()) return;
		send(`/app/chat/${roomId}`, { content: input, messageType: 'TEXT' });
		input = '';
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter' && !e.shiftKey) {
			e.preventDefault();
			if (showMentionSuggest) {
				const filtered = roomMembers.filter((m) =>
					m.userName?.toLowerCase().includes(mentionQuery.toLowerCase()) && m.userId !== auth.user?.sub
				);
				if (filtered.length > 0) selectMention(filtered[0]);
				return;
			}
			handleSend();
		} else {
			sendTyping();
		}
	}

	function handleInput() {
		const atMatch = input.match(/@(\w*)$/);
		if (atMatch) { mentionQuery = atMatch[1]; showMentionSuggest = true; }
		else { showMentionSuggest = false; }
	}

	function selectMention(member: {userId: string, userName: string}) {
		input = input.replace(/@\w*$/, `@${member.userName} `);
		showMentionSuggest = false;
	}

	async function handleFileUpload() {
		const file = fileInput?.files?.[0];
		if (!file) return;

		try {
			const { uploadUrl, s3Key } = await getUploadUrl(roomId, file.name, file.type);
			await fetch(uploadUrl, { method: 'PUT', body: file, headers: { 'Content-Type': file.type } });
			send(`/app/chat/${roomId}`, {
				content: s3Key,
				messageType: file.type.startsWith('image/') ? 'IMAGE' : 'FILE'
			});
		} catch (e) {
			console.error('Upload failed:', e);
		}
		fileInput.value = '';
	}

	async function handleSearch() {
		if (!searchQuery.trim()) return;
		searching = true;
		try {
			const res = await searchMessages(roomId, searchQuery);
			searchResults = res.content;
		} catch {
			searchResults = [];
		} finally {
			searching = false;
		}
	}

	async function handleLeave() {
		await leaveRoom(roomId);
		goto('/rooms');
	}

	async function handleDelete() {
		if (!confirm('このルームを削除しますか？メッセージも全て削除されます。')) return;
		try {
			await deleteRoom(roomId);
			goto('/rooms');
		} catch {
			// not owner or error
		}
	}

	async function resolveImageUrl(msg: Message) {
		if (msg.messageType === 'IMAGE' && !imageUrls[msg.id]) {
			try {
				const { downloadUrl } = await getDownloadUrl(msg.content);
				imageUrls = { ...imageUrls, [msg.id]: downloadUrl };
			} catch {
				// ignore
			}
		}
	}

	function isOwnMessage(msg: Message) {
		return msg.senderId === auth.user?.sub;
	}

	function formatDate(dateStr: string) {
		const d = new Date(dateStr);
		const now = new Date();
		const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
		const yesterday = new Date(today.getTime() - 86400000);
		const msgDate = new Date(d.getFullYear(), d.getMonth(), d.getDate());
		if (msgDate.getTime() === today.getTime()) return '今日';
		if (msgDate.getTime() === yesterday.getTime()) return '昨日';
		return d.toLocaleDateString('ja-JP', { month: 'long', day: 'numeric' });
	}

	function shouldShowDateSeparator(msgs: Message[], index: number) {
		if (index === 0) return true;
		const prev = new Date(msgs[index - 1].createdAt).toDateString();
		const curr = new Date(msgs[index].createdAt).toDateString();
		return prev !== curr;
	}

	$effect(() => {
		if (auth.isAuthenticated && roomId) {
			untrack(() => {
				loadRoom();
				setCurrentRoom(roomId);
				clearUnread(roomId);
			});
		}
		return () => setCurrentRoom(null);
	});

	$effect(() => {
		if (ws.connected && roomId) {
			const sub = untrack(() => subscribeToRoom());
			const typingSub = untrack(() => subscribeToTyping());
			return () => { sub?.unsubscribe(); typingSub?.unsubscribe(); };
		}
	});
</script>

<div class="flex h-screen flex-col overflow-hidden">
	<!-- Header -->
	<header class="flex shrink-0 items-center justify-between border-b border-border bg-background px-4 py-3 sm:px-6">
		<div class="flex items-center gap-3">
			<a href="/rooms" class="text-muted-foreground hover:text-foreground/80">&larr;</a>
			<h1 class="font-bold">
				<span class="text-primary">#</span> {room?.name || '...'}
			</h1>
		</div>
		<div class="flex items-center gap-2">
			<button
				onclick={() => { showSearch = !showSearch; searchQuery = ''; searchResults = []; }}
				class="rounded px-3 py-1 text-xs text-muted-foreground hover:bg-muted hover:text-foreground/80"
			>
				検索
			</button>
			<button
				onclick={handleLeave}
				class="rounded px-3 py-1 text-xs text-muted-foreground hover:bg-red-950 hover:text-red-400"
			>
				退出
			</button>
			{#if room?.createdBy === auth.user?.sub}
				<button
					onclick={handleDelete}
					class="rounded px-3 py-1 text-xs text-muted-foreground hover:bg-red-950 hover:text-red-400"
				>
					削除
				</button>
			{/if}
		</div>
	</header>

	<!-- Search Panel -->
	{#if showSearch}
		<div class="shrink-0 border-b border-border bg-background px-4 py-3 sm:px-6">
			<form onsubmit={(e) => { e.preventDefault(); handleSearch(); }} class="flex gap-2">
				<input
					type="text"
					bind:value={searchQuery}
					placeholder="メッセージを検索..."
					class="flex-1 rounded-lg border border-input bg-card px-3 py-2 text-sm text-foreground placeholder-muted-foreground focus:border-primary focus:outline-none"
				/>
				<button
					type="submit"
					disabled={searching || !searchQuery.trim()}
					class="rounded-lg bg-zinc-700 px-4 py-2 text-sm text-foreground hover:bg-zinc-600 disabled:opacity-50"
				>
					{searching ? '検索中...' : '検索'}
				</button>
			</form>
			{#if searchResults.length > 0}
				<div class="mt-3 max-h-48 overflow-y-auto">
					{#each searchResults as result (result.id)}
						<div class="border-b border-border py-2 last:border-0">
							<span class="text-xs text-primary">{result.senderName}</span>
							<span class="ml-2 text-xs text-muted-foreground/60">{new Date(result.createdAt).toLocaleString('ja-JP')}</span>
							<p class="mt-1 text-sm text-foreground/80">{result.content}</p>
						</div>
					{/each}
				</div>
			{:else if searchQuery && !searching}
				<p class="mt-2 text-xs text-muted-foreground">結果なし</p>
			{/if}
		</div>
	{/if}

	<!-- Messages -->
	<div class="flex-1 overflow-y-auto px-4 py-4 sm:px-6" bind:this={messagesContainer} onscroll={handleScroll}>
		{#if loadingMore}
			<p class="py-2 text-center text-xs text-muted-foreground/60">読み込み中...</p>
		{/if}
		{#if messages.length === 0}
			<p class="text-center text-sm text-muted-foreground/60">メッセージはまだありません</p>
		{/if}
		{#each messages as msg, i (msg.id)}
			{#if shouldShowDateSeparator(messages, i)}
				<div class="my-4 flex items-center gap-3">
					<div class="h-px flex-1 bg-border"></div>
					<span class="text-xs text-muted-foreground/60">{formatDate(msg.createdAt)}</span>
					<div class="h-px flex-1 bg-border"></div>
				</div>
			{/if}
			<div class="group mb-3 {isOwnMessage(msg) ? 'text-right' : ''}">
				{#if !isOwnMessage(msg)}
					<span class="text-xs text-muted-foreground">{msg.senderName}</span>
				{/if}
				<div class="{isOwnMessage(msg)
					? 'ml-auto bg-primary/10 border-primary/30'
					: 'mr-auto bg-muted/50 border-border'} inline-block max-w-xs rounded-lg border px-3 py-2 text-left sm:max-w-md">
					{#if msg.messageType === 'SYSTEM'}
						<p class="text-xs italic text-muted-foreground">{msg.content}</p>
					{:else if msg.messageType === 'IMAGE'}
						{#if imageUrls[msg.id]}
							<img src={imageUrls[msg.id]} alt="画像" class="max-w-full rounded" loading="lazy" />
						{:else}
							<p class="text-sm text-muted-foreground">読み込み中...</p>
						{/if}
					{:else if msg.messageType === 'FILE'}
						<p class="text-sm text-foreground/80">ファイル</p>
					{:else}
						{#if editingId === msg.id}
							<div class="flex gap-1">
								<input type="text" bind:value={editContent} class="flex-1 rounded bg-background px-2 py-1 text-sm text-foreground" />
								<button onclick={handleEditSave} class="text-xs text-primary">保存</button>
								<button onclick={() => { editingId = null; }} class="text-xs text-muted-foreground">取消</button>
							</div>
						{:else}
							<p class="text-sm text-foreground">{@html msg.content.replace(/@(\w+)/g, '<span class="font-medium text-primary">@$1</span>')}</p>
						{/if}
					{/if}
					{#if isOwnMessage(msg) && msg.messageType === 'TEXT' && editingId !== msg.id}
						<div class="mt-1 flex gap-2">
							<button onclick={() => handleEdit(msg)} class="text-[10px] text-muted-foreground/40 hover:text-muted-foreground">編集</button>
							<button onclick={() => handleDeleteMessage(msg.id)} class="text-[10px] text-muted-foreground/40 hover:text-red-400">削除</button>
						</div>
					{/if}
				</div>
				<div class="mt-0.5 flex items-center gap-1 text-[10px] text-muted-foreground/60">
					{new Date(msg.createdAt).toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit' })}
					{#if isOwnMessage(msg) && Object.entries(readStatus).some(([uid, mid]) => uid !== auth.user?.sub && mid >= msg.id)}
						<span class="ml-1 text-primary/60">既読</span>
					{/if}
					{#each QUICK_EMOJIS.slice(0, 3) as emoji}
						<button onclick={() => toggleReaction(msg.id, emoji)} class="ml-1 opacity-0 transition hover:opacity-100 group-hover:opacity-50">{emoji}</button>
					{/each}
				</div>
				{#if reactions[msg.id]?.length}
					<div class="mt-1 flex flex-wrap gap-1">
						{#each reactions[msg.id] as r}
							<button
								onclick={() => toggleReaction(msg.id, r.emoji)}
								class="rounded-full border px-1.5 py-0.5 text-xs {r.userIds.includes(auth.user?.sub || '') ? 'border-primary/50 bg-primary/10' : 'border-border bg-muted/30'}"
							>
								{r.emoji} {r.count}
							</button>
						{/each}
					</div>
				{/if}
			</div>
		{/each}
		<div bind:this={messagesEnd}></div>
	</div>

	<!-- Typing indicator -->
	{#if Object.keys(typingUsers).length > 0}
		<div class="shrink-0 px-4 py-1 text-xs text-muted-foreground/60 sm:px-6">
			{Object.values(typingUsers).join(', ')} が入力中...
		</div>
	{/if}

	<!-- Mention suggest -->
	{#if showMentionSuggest}
		<div class="shrink-0 border-t border-border bg-card px-4 py-2 sm:px-6">
			{#each roomMembers.filter((m) => m.userName?.toLowerCase().includes(mentionQuery.toLowerCase()) && m.userId !== auth.user?.sub) as member}
				<button onclick={() => selectMention(member)} class="block w-full rounded px-2 py-1 text-left text-sm text-foreground hover:bg-muted">@{member.userName}</button>
			{/each}
		</div>
	{/if}

	<!-- Input -->
	<div class="shrink-0 border-t border-border bg-background px-4 py-3 sm:px-6">
		<div class="flex items-center gap-2">
			<button
				onclick={() => fileInput.click()}
				class="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-foreground/80"
			>
				<HugeiconsIcon icon={Attachment01Icon} size={18} />
			</button>
			<input type="file" bind:this={fileInput} onchange={handleFileUpload} class="hidden" />
			<input
				type="text"
				bind:value={input}
				onkeydown={handleKeydown}
				oninput={handleInput}
				placeholder="メッセージを入力... (@でメンション)"
				class="flex-1 rounded-lg border border-input bg-card px-4 py-2 text-foreground placeholder-muted-foreground focus:border-primary focus:outline-none"
			/>
			<button
				onclick={handleSend}
				disabled={!input.trim()}
				class="shrink-0 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-white hover:bg-primary/80 disabled:opacity-50 sm:px-4"
			>
				送信
			</button>
		</div>
	</div>
</div>
