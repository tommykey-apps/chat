<script lang="ts">
	import { goto } from '$app/navigation';
	import { listRooms, createRoom, joinRoom, type Room } from '$lib/api';
	import { getAuthState } from '$lib/stores/auth.svelte';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import * as Card from '$lib/components/ui/card';
	import * as Dialog from '$lib/components/ui/dialog';
	import { Badge } from '$lib/components/ui/badge';
	import * as Alert from '$lib/components/ui/alert';

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
	<header class="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background px-4 py-3 sm:px-6">
		<div class="flex items-center gap-4">
			<h1 class="text-lg font-bold"><span class="text-primary">#</span> チャットルーム</h1>
			<a href="/friends" class="text-sm text-muted-foreground hover:text-foreground">フレンド</a>
		</div>
		<Button onclick={() => (showCreate = !showCreate)}>新規作成</Button>
	</header>

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
				<Card.Content class="py-8 text-center">
					<p class="text-muted-foreground">まだルームがありません。作成してみましょう</p>
				</Card.Content>
			</Card.Root>
		{:else}
			<div class="flex flex-col gap-3">
				{#each rooms as room (room.id)}
					<a href="/rooms/{room.id}" class="block">
						<Card.Root class="transition hover:border-primary/30 hover:bg-card/80">
							<Card.Content class="pt-4">
								<div class="flex items-center justify-between">
									<h2 class="font-medium">
										<span class="text-primary">#</span> {room.name}
									</h2>
									<Badge variant="secondary">{room.memberCount} 人</Badge>
								</div>
								{#if room.description}
									<p class="mt-1 text-sm text-muted-foreground">{room.description}</p>
								{/if}
							</Card.Content>
						</Card.Root>
					</a>
				{/each}
			</div>
		{/if}
	</main>
</div>
