<script lang="ts">
	import { goto } from '$app/navigation';
	import { signUp, confirmSignUp, login as cognitoLogin, forgotPassword, confirmNewPassword } from '$lib/auth';
	import { setAuth } from '$lib/stores/auth.svelte';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import * as Card from '$lib/components/ui/card';
	import * as Tabs from '$lib/components/ui/tabs';
	import * as Alert from '$lib/components/ui/alert';

	let mode = $state<'login' | 'signup' | 'confirm' | 'forgot' | 'reset'>('login');
	let email = $state('');
	let password = $state('');
	let confirmCode = $state('');
	let newPassword = $state('');
	let error = $state('');
	let loading = $state(false);

	async function handleLogin() {
		loading = true;
		error = '';
		try {
			const session = await cognitoLogin(email, password);
			const idToken = session.getIdToken();
			setAuth(
				{ sub: idToken.payload.sub!, email: idToken.payload.email as string },
				session.getAccessToken().getJwtToken()
			);
			goto('/rooms');
		} catch (e) {
			error = e instanceof Error ? e.message : 'ログインに失敗しました';
		} finally {
			loading = false;
		}
	}

	async function handleSignUp() {
		loading = true;
		error = '';
		try {
			await signUp(email, password);
			mode = 'confirm';
		} catch (e) {
			error = e instanceof Error ? e.message : 'サインアップに失敗しました';
		} finally {
			loading = false;
		}
	}

	async function handleConfirm() {
		loading = true;
		error = '';
		try {
			await confirmSignUp(email, confirmCode);
			mode = 'login';
		} catch (e) {
			error = e instanceof Error ? e.message : '認証コードが正しくありません';
		} finally {
			loading = false;
		}
	}

	async function handleForgot() {
		loading = true;
		error = '';
		try {
			await forgotPassword(email);
			mode = 'reset';
		} catch (e) {
			error = e instanceof Error ? e.message : 'パスワードリセットに失敗しました';
		} finally {
			loading = false;
		}
	}

	async function handleReset() {
		loading = true;
		error = '';
		try {
			await confirmNewPassword(email, confirmCode, newPassword);
			mode = 'login';
		} catch (e) {
			error = e instanceof Error ? e.message : 'パスワードリセットに失敗しました';
		} finally {
			loading = false;
		}
	}
</script>

<div class="flex min-h-screen items-center justify-center px-4">
	<Card.Root class="w-full max-w-sm">
		<Card.Header class="text-center">
			<Card.Title class="text-2xl">
				<span class="text-primary">#</span> chatto
			</Card.Title>
		</Card.Header>
		<Card.Content>
			{#if mode === 'confirm'}
				<form onsubmit={(e) => { e.preventDefault(); handleConfirm(); }} class="flex flex-col gap-4">
					<p class="text-sm text-muted-foreground">メールに届いた認証コードを入力してください</p>
					<Input
						type="text"
						bind:value={confirmCode}
						placeholder="認証コード"
						required
					/>
					<Button type="submit" disabled={loading} class="w-full">
						{loading ? '確認中...' : '確認'}
					</Button>
				</form>
			{:else if mode === 'forgot'}
				<form onsubmit={(e) => { e.preventDefault(); handleForgot(); }} class="flex flex-col gap-4">
					<p class="text-sm text-muted-foreground">メールアドレスを入力してください。リセットコードを送信します。</p>
					<Input type="email" bind:value={email} placeholder="メールアドレス" required />
					<Button type="submit" disabled={loading} class="w-full">
						{loading ? '送信中...' : 'リセットコード送信'}
					</Button>
					<button type="button" onclick={() => { mode = 'login'; error = ''; }} class="text-xs text-muted-foreground hover:text-foreground">
						ログインに戻る
					</button>
				</form>
			{:else if mode === 'reset'}
				<form onsubmit={(e) => { e.preventDefault(); handleReset(); }} class="flex flex-col gap-4">
					<p class="text-sm text-muted-foreground">メールに届いたコードと新しいパスワードを入力してください</p>
					<Input type="text" bind:value={confirmCode} placeholder="リセットコード" required />
					<Input type="password" bind:value={newPassword} placeholder="新しいパスワード" required minlength={8} />
					<Button type="submit" disabled={loading} class="w-full">
						{loading ? '処理中...' : 'パスワードをリセット'}
					</Button>
				</form>
			{:else}
				<Tabs.Root value={mode} onValueChange={(v) => { mode = v as 'login' | 'signup'; error = ''; }}>
					<Tabs.List class="mb-6 w-full">
						<Tabs.Trigger value="login" class="flex-1">ログイン</Tabs.Trigger>
						<Tabs.Trigger value="signup" class="flex-1">サインアップ</Tabs.Trigger>
					</Tabs.List>
				</Tabs.Root>

				<form
					onsubmit={(e) => { e.preventDefault(); mode === 'login' ? handleLogin() : handleSignUp(); }}
					class="flex flex-col gap-4"
				>
					<Input
						type="email"
						bind:value={email}
						placeholder="メールアドレス"
						required
					/>
					<Input
						type="password"
						bind:value={password}
						placeholder="パスワード"
						required
						minlength={8}
					/>
					{#if mode === 'signup'}
						<p class="text-xs text-muted-foreground">8文字以上、大文字・小文字・数字を含む</p>
					{/if}
					<Button type="submit" disabled={loading} class="w-full">
						{loading ? '処理中...' : mode === 'login' ? 'ログイン' : 'サインアップ'}
					</Button>
					{#if mode === 'login'}
						<button type="button" onclick={() => { mode = 'forgot'; error = ''; }} class="text-xs text-muted-foreground hover:text-foreground">
							パスワードを忘れた方
						</button>
					{/if}
				</form>
			{/if}

			{#if error}
				<Alert.Root variant="destructive" class="mt-4">
					<Alert.Description>{error}</Alert.Description>
				</Alert.Root>
			{/if}
		</Card.Content>
	</Card.Root>
</div>
