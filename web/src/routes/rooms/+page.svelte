<script lang="ts">
	import { goto } from '$app/navigation';
	import { listRooms, createRoom, joinRoom, searchAllMessages, type Room, type SearchResult } from '$lib/api';
	import { getAuthState, logout } from '$lib/stores/auth.svelte';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import * as Card from '$lib/components/ui/card';
	import * as Dialog from '$lib/components/ui/dialog';
	import { Badge } from '$lib/components/ui/badge';
	import * as Alert from '$lib/components/ui/alert';
	import { getNotificationState } from '$lib/stores/notifications.svelte';

	const notifications = getNotificationState();
	let rooms = $state<Room[]>([]);
	let showCreate = $state(false);
	let newName = $state('');
	let newDesc = $state('');
	let loading = $state(false);
	let error = $state('');
	let showSearch = $state(false);
	let searchQuery = $state('');
	let searchResults = $state<SearchResult[]>([]);
	let searching = $state(false);

	async function handleSearch() {
		if (!searchQuery.trim()) return;
		searching = true;
		try {
			const res = await searchAllMessages(searchQuery);
			searchResults = res.content;
		} catch {
			searchResults = [];
		} finally {
			searching = false;
		}
	}

	function getRoomName(roomId: string): string {
		const room = rooms.find(r => r.id === roomId);
		return room ? room.name : 'Unknown';
	}

	async function loadRooms() {
		try {
			rooms = await listRooms();
		} catch {
			rooms = [];
		}
	}

	async function handleCreate() {
		if (!newName.trim()) return;
		loading = true;
		error = '';
		try {
			const room = await createRoom(newName, newDesc);
			newName = '';
			newDesc = '';
			showCreate = false;
			goto(`/rooms/${room.id}`);
		} catch (e) {
			error = e instanceof Error ? e.message : 'ルーム作成に失敗しました';
		} finally {
			loading = false;
		}
	}

	$effect(() => {
		if (getAuthState().isAuthenticated) loadRooms();
	});
</script>

<div class="flex min-h-screen flex-col">
	<header class="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background px-3 py-3 sm:px-6">
		<div class="flex items-center gap-2 sm:gap-4">
			<h1 class="text-base font-bold sm:text-lg"><span class="text-primary">#</span> <span class="hidden sm:inline">チャット</span>ルーム</h1>
			<a href="/friends" class="text-xs text-muted-foreground hover:text-foreground sm:text-sm">フレンド</a>
			<a href="/profile" class="text-xs text-muted-foreground hover:text-foreground sm:text-sm">設定</a>
		</div>
		<div class="flex items-center gap-1 sm:gap-2">
			<Button size="sm" variant="ghost" onclick={() => { showSearch = !showSearch; if (!showSearch) { searchQuery = ''; searchResults = []; } }}>検索</Button>
			<Button size="sm" onclick={() => (showCreate = !showCreate)}>+</Button>
			<button
				onclick={() => { logout(); goto('/login'); }}
				class="rounded px-2 py-1 text-[10px] text-muted-foreground hover:bg-red-950 hover:text-red-400 sm:text-xs sm:px-3"
			>
				logout
			</button>
		</div>
	</header>

	{#if showSearch}
		<div class="mx-auto w-full max-w-2xl px-4 pt-4 sm:px-6">
			<form onsubmit={(e) => { e.preventDefault(); handleSearch(); }} class="flex gap-2">
				<Input bind:value={searchQuery} placeholder="全ルーム横断検索..." class="flex-1" />
				<Button type="submit" disabled={searching}>{searching ? '検索中...' : '検索'}</Button>
			</form>
			{#if searchResults.length > 0}
				<div class="mt-3 flex flex-col gap-2">
					{#each searchResults as result (result.id)}
						<a href="/rooms/{result.roomId}" class="block">
							<Card.Root class="py-2 gap-1 transition hover:border-primary/30 hover:bg-card/80">
								<Card.Content>
									<div class="flex items-center gap-2 text-xs text-muted-foreground">
										<span class="text-primary font-medium">#{getRoomName(result.roomId)}</span>
										<span>{result.senderName}</span>
										<span>{new Date(result.createdAt).toLocaleString('ja-JP', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>
									</div>
									<p class="mt-1 text-sm">{@html result.highlightedContent}</p>
								</Card.Content>
							</Card.Root>
						</a>
					{/each}
				</div>
			{:else if searchQuery && !searching}
				<p class="mt-3 text-center text-sm text-muted-foreground">結果なし</p>
			{/if}
		</div>
	{/if}

	<main class="mx-auto w-full max-w-2xl flex-1 px-4 py-6 sm:px-6">
		<Dialog.Root bind:open={showCreate}>
			<Dialog.Content>
				<Dialog.Header>
					<Dialog.Title>新規ルーム作成</Dialog.Title>
				</Dialog.Header>
				<form onsubmit={(e) => { e.preventDefault(); handleCreate(); }} class="flex flex-col gap-3">
					<Input bind:value={newName} placeholder="ルーム名" required />
					<Input bind:value={newDesc} placeholder="説明（任意）" />
					<div class="flex gap-2">
						<Button type="submit" disabled={loading}>
							{loading ? '作成中...' : '作成'}
						</Button>
						<Button variant="ghost" type="button" onclick={() => (showCreate = false)}>
							キャンセル
						</Button>
					</div>
				</form>
				{#if error}
					<Alert.Root variant="destructive" class="mt-2">
						<Alert.Description>{error}</Alert.Description>
					</Alert.Root>
				{/if}
			</Dialog.Content>
		</Dialog.Root>

		{#if rooms.length === 0}
			<Card.Root class="border-dashed">
				<Card.Content class="py-12 text-center">
					<p class="text-3xl">💬</p>
					<p class="mt-3 font-medium text-foreground">まだルームがありません</p>
					<p class="mt-1 text-sm text-muted-foreground">フレンドを追加してチャットを始めましょう</p>
					<div class="mt-6 flex justify-center gap-3">
						<Button onclick={() => (showCreate = !showCreate)}>ルームを作成</Button>
						<Button variant="outline" onclick={() => goto('/friends')}>フレンドを探す</Button>
					</div>
				</Card.Content>
			</Card.Root>
		{:else}
			<div class="flex flex-col gap-3">
				{#each rooms as room (room.id)}
					<a href="/rooms/{room.id}" class="block">
						<Card.Root class="py-3 gap-2 transition hover:border-primary/30 hover:bg-card/80">
							<Card.Content>
								<div class="flex items-center justify-between">
									<h2 class="font-medium">
										<span class="text-primary">#</span> {room.name}
									</h2>
									<div class="flex items-center gap-2">
										{#if notifications.unreadCounts[room.id]}
											<Badge variant="destructive">{notifications.unreadCounts[room.id]}</Badge>
										{/if}
										<Badge variant="secondary">{room.memberCount} 人</Badge>
									</div>
								</div>
								{#if room.lastMessage}
									<p class="mt-1 truncate text-sm text-muted-foreground">{room.lastMessage}</p>
								{:else if room.description}
									<p class="mt-1 text-sm text-muted-foreground/50">{room.description}</p>
								{/if}
							</Card.Content>
						</Card.Root>
					</a>
				{/each}
			</div>
		{/if}
	</main>
</div>
