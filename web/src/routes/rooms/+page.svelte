<script lang="ts">
	import { goto } from '$app/navigation';
	import { listRooms, createRoom, joinRoom, type Room } from '$lib/api';
	import { getAuthState } from '$lib/stores/auth.svelte';

	let rooms = $state<Room[]>([]);
	let showCreate = $state(false);
	let newName = $state('');
	let newDesc = $state('');
	let loading = $state(false);
	let error = $state('');

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
	<!-- Header -->
	<header class="sticky top-0 z-10 flex items-center justify-between border-b border-zinc-800 bg-zinc-950 px-4 py-3 sm:px-6">
		<div class="flex items-center gap-4">
			<h1 class="text-lg font-bold"><span class="text-emerald-400">#</span> チャットルーム</h1>
			<a href="/friends" class="text-sm text-zinc-500 hover:text-zinc-300">フレンド</a>
		</div>
		<button
			onclick={() => (showCreate = !showCreate)}
			class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500"
		>
			新規作成
		</button>
	</header>

	<main class="mx-auto w-full max-w-2xl flex-1 px-4 py-6 sm:px-6">
		<!-- Create Form -->
		{#if showCreate}
			<div class="mb-6 rounded-lg border border-zinc-800 bg-zinc-900/30 p-4">
				<form onsubmit={(e) => { e.preventDefault(); handleCreate(); }} class="flex flex-col gap-3">
					<input
						type="text"
						bind:value={newName}
						placeholder="ルーム名"
						required
						class="rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-zinc-50 placeholder-zinc-500 focus:border-emerald-500 focus:outline-none"
					/>
					<input
						type="text"
						bind:value={newDesc}
						placeholder="説明（任意）"
						class="rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-zinc-50 placeholder-zinc-500 focus:border-emerald-500 focus:outline-none"
					/>
					<div class="flex gap-2">
						<button
							type="submit"
							disabled={loading}
							class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
						>
							{loading ? '作成中...' : '作成'}
						</button>
						<button
							type="button"
							onclick={() => (showCreate = false)}
							class="rounded-lg px-4 py-2 text-sm text-zinc-400 hover:text-zinc-200"
						>
							キャンセル
						</button>
					</div>
				</form>
				{#if error}
					<p class="mt-2 text-sm text-red-400">{error}</p>
				{/if}
			</div>
		{/if}

		<!-- Room List -->
		{#if rooms.length === 0}
			<div class="rounded-lg border border-dashed border-zinc-700 p-8 text-center">
				<p class="text-zinc-500">まだルームがありません。作成してみましょう</p>
			</div>
		{:else}
			<div class="flex flex-col gap-3">
				{#each rooms as room (room.id)}
					<a
						href="/rooms/{room.id}"
						class="rounded-lg border border-zinc-800 bg-zinc-900/30 p-4 transition hover:border-zinc-700 hover:bg-zinc-900/50"
					>
						<div class="flex items-center justify-between">
							<h2 class="font-medium text-zinc-50">
								<span class="text-emerald-400">#</span> {room.name}
							</h2>
							<span class="text-xs text-zinc-500">{room.memberCount} 人</span>
						</div>
						{#if room.description}
							<p class="mt-1 text-sm text-zinc-400">{room.description}</p>
						{/if}
					</a>
				{/each}
			</div>
		{/if}
	</main>
</div>
