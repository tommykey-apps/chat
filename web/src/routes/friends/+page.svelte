<script lang="ts">
	import { goto } from '$app/navigation';
	import {
		listFriends, listFriendRequests, searchUsers, sendFriendRequest,
		acceptFriendRequest, removeFriend, createRoom, listRooms,
		type UserInfo, type FriendRequest
	} from '$lib/api';
	import { getAuthState } from '$lib/stores/auth.svelte';

	let tab = $state<'friends' | 'requests'>('friends');
	let friends = $state<UserInfo[]>([]);
	let requests = $state<FriendRequest[]>([]);
	let searchQuery = $state('');
	let searchResults = $state<UserInfo[]>([]);
	let searching = $state(false);
	let message = $state('');

	const auth = getAuthState();

	async function loadFriends() {
		friends = await listFriends();
	}

	async function loadRequests() {
		requests = await listFriendRequests();
	}

	async function handleSearch() {
		if (!searchQuery.trim()) return;
		searching = true;
		message = '';
		try {
			searchResults = (await searchUsers(searchQuery)).filter(u => u.id !== auth.user?.sub);
		} catch {
			searchResults = [];
		} finally {
			searching = false;
		}
	}

	async function handleSendRequest(userId: string) {
		try {
			await sendFriendRequest(userId);
			message = '申請を送信しました';
			searchResults = searchResults.filter(u => u.id !== userId);
		} catch (e) {
			message = e instanceof Error ? e.message : '送信に失敗しました';
		}
	}

	async function handleAccept(userId: string) {
		await acceptFriendRequest(userId);
		await loadRequests();
		await loadFriends();
	}

	async function handleReject(userId: string) {
		await removeFriend(userId);
		await loadRequests();
	}

	async function handleStartChat(friend: UserInfo) {
		const rooms = await listRooms();
		const existing = rooms.find(
			(r) => r.description === 'DM' && r.memberCount === 2 && r.name === friend.displayName
		);
		if (existing) {
			goto(`/rooms/${existing.id}`);
			return;
		}
		const room = await createRoom(`${friend.displayName}`, 'DM', [friend.id]);
		goto(`/rooms/${room.id}`);
	}

	$effect(() => {
		if (auth.isAuthenticated) {
			loadFriends();
			loadRequests();
		}
	});
</script>

<div class="flex min-h-screen flex-col">
	<header class="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background px-4 py-3 sm:px-6">
		<h1 class="text-lg font-bold"><span class="text-primary">@</span> フレンド</h1>
		<a href="/rooms" class="rounded-lg px-4 py-2 text-sm text-muted-foreground hover:text-foreground">ルーム一覧</a>
	</header>

	<main class="mx-auto w-full max-w-2xl flex-1 px-4 py-6 sm:px-6">
		<!-- Search -->
		<div class="mb-6">
			<form onsubmit={(e) => { e.preventDefault(); handleSearch(); }} class="flex gap-2">
				<input
					type="text"
					bind:value={searchQuery}
					placeholder="メールアドレスまたは名前で検索..."
					class="flex-1 rounded-lg border border-input bg-card px-4 py-2 text-foreground placeholder-muted-foreground focus:border-primary focus:outline-none"
				/>
				<button
					type="submit"
					disabled={searching || !searchQuery.trim()}
					class="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/80 disabled:opacity-50"
				>
					検索
				</button>
			</form>
			{#if message}
				<p class="mt-2 text-sm text-primary">{message}</p>
			{/if}
			{#if searchResults.length > 0}
				<div class="mt-3 flex flex-col gap-2">
					{#each searchResults as user (user.id)}
						<div class="flex items-center justify-between rounded-lg border border-border bg-card p-3">
							<div>
								<span class="text-sm font-medium text-foreground">{user.displayName}</span>
								<span class="ml-2 text-xs text-muted-foreground">{user.email}</span>
							</div>
							<button
								onclick={() => handleSendRequest(user.id)}
								class="rounded-lg bg-zinc-700 px-3 py-1 text-xs text-foreground hover:bg-zinc-600"
							>
								申請
							</button>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<!-- Tabs -->
		<div class="mb-4 flex gap-4 border-b border-border">
			<button
				onclick={() => (tab = 'friends')}
				class="border-b-2 px-2 pb-2 text-sm {tab === 'friends' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground/80'}"
			>
				フレンド ({friends.length})
			</button>
			<button
				onclick={() => (tab = 'requests')}
				class="border-b-2 px-2 pb-2 text-sm {tab === 'requests' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground/80'}"
			>
				申請 ({requests.length})
			</button>
		</div>

		<!-- Friends List -->
		{#if tab === 'friends'}
			{#if friends.length === 0}
				<div class="rounded-lg border border-dashed border-input p-8 text-center">
					<p class="text-muted-foreground">まだフレンドがいません。上の検索から追加してみましょう</p>
				</div>
			{:else}
				<div class="flex flex-col gap-3">
					{#each friends as friend (friend.id)}
						<div class="flex items-center justify-between rounded-lg border border-border bg-card p-4">
							<div>
								<span class="font-medium text-foreground">{friend.displayName}</span>
								<span class="ml-2 text-xs text-muted-foreground">{friend.email}</span>
							</div>
							<div class="flex gap-2">
								<button
									onclick={() => handleStartChat(friend)}
									class="rounded-lg bg-primary px-3 py-1 text-xs text-white hover:bg-primary/80"
								>
									チャット
								</button>
								<button
									onclick={() => { removeFriend(friend.id); loadFriends(); }}
									class="rounded-lg px-3 py-1 text-xs text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
								>
									削除
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		{/if}

		<!-- Requests -->
		{#if tab === 'requests'}
			{#if requests.length === 0}
				<div class="rounded-lg border border-dashed border-input p-8 text-center">
					<p class="text-muted-foreground">フレンド申請はありません</p>
				</div>
			{:else}
				<div class="flex flex-col gap-3">
					{#each requests as req (req.userId)}
						<div class="flex items-center justify-between rounded-lg border border-border bg-card p-4">
							<div>
								<span class="font-medium text-foreground">{req.displayName}</span>
								<span class="ml-2 text-xs text-muted-foreground">{req.email}</span>
							</div>
							<div class="flex gap-2">
								<button
									onclick={() => handleAccept(req.userId)}
									class="rounded-lg bg-primary px-3 py-1 text-xs text-white hover:bg-primary/80"
								>
									承認
								</button>
								<button
									onclick={() => handleReject(req.userId)}
									class="rounded-lg px-3 py-1 text-xs text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
								>
									拒否
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		{/if}
	</main>
</div>
