<script lang="ts">
	import { goto } from '$app/navigation';
	import { getMe, updateProfile, type UserInfo } from '$lib/api';
	import { getAuthState } from '$lib/stores/auth.svelte';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import * as Card from '$lib/components/ui/card';
	import * as Alert from '$lib/components/ui/alert';

	const auth = getAuthState();
	let user = $state<UserInfo | null>(null);
	let displayName = $state('');
	let saving = $state(false);
	let success = $state(false);
	let error = $state('');

	async function loadProfile() {
		try {
			user = await getMe();
			displayName = user.displayName;
		} catch {
			// ignore
		}
	}

	async function handleSave() {
		if (!displayName.trim()) return;
		saving = true;
		error = '';
		success = false;
		try {
			user = await updateProfile(displayName.trim());
			displayName = user.displayName;
			success = true;
			setTimeout(() => (success = false), 3000);
		} catch (e) {
			error = e instanceof Error ? e.message : '更新に失敗しました';
		} finally {
			saving = false;
		}
	}

	$effect(() => {
		if (auth.isAuthenticated) loadProfile();
	});
</script>

<div class="flex min-h-screen flex-col">
	<header class="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background px-4 py-3 sm:px-6">
		<h1 class="text-lg font-bold">プロフィール設定</h1>
		<a href="/rooms" class="text-sm text-muted-foreground hover:text-foreground">ルーム一覧</a>
	</header>

	<main class="mx-auto w-full max-w-md flex-1 px-4 py-6 sm:px-6">
		{#if user}
			<Card.Root>
				<Card.Header>
					<Card.Title>アカウント情報</Card.Title>
				</Card.Header>
				<Card.Content class="flex flex-col gap-4">
					<div>
						<label class="text-xs text-muted-foreground">メールアドレス</label>
						<p class="text-sm text-foreground">{user.email}</p>
					</div>
					<div>
						<label for="displayName" class="text-xs text-muted-foreground">表示名</label>
						<Input id="displayName" bind:value={displayName} placeholder="表示名を入力" />
					</div>
					<Button onclick={handleSave} disabled={saving || !displayName.trim()}>
						{saving ? '保存中...' : '保存'}
					</Button>
					{#if success}
						<Alert.Root>
							<Alert.Description>表示名を更新しました</Alert.Description>
						</Alert.Root>
					{/if}
					{#if error}
						<Alert.Root variant="destructive">
							<Alert.Description>{error}</Alert.Description>
						</Alert.Root>
					{/if}
				</Card.Content>
			</Card.Root>
		{/if}
	</main>
</div>
