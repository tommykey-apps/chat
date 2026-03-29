<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import '../app.css';
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { initAuth, getAuthState } from '$lib/stores/auth.svelte';

	let { children } = $props();

	const publicPaths = ['/login'];
	const auth = getAuthState();
	const isPublic = $derived(publicPaths.includes(page.url.pathname));
	const needsRedirect = $derived(!auth.loading && !auth.isAuthenticated && !isPublic);

	$effect(() => {
		initAuth();
	});

	$effect(() => {
		if (needsRedirect) goto('/login');
	});

	$effect(() => {
		document.documentElement.classList.add('dark');
	});
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Chat</title>
	<meta name="viewport" content="width=device-width, initial-scale=1" />
</svelte:head>

{#if auth.loading}
	<div class="flex min-h-screen items-center justify-center bg-background text-foreground">
		<p class="text-muted-foreground">Loading...</p>
	</div>
{:else if needsRedirect}
	<div class="flex min-h-screen items-center justify-center bg-background text-foreground">
		<p class="text-muted-foreground">Redirecting...</p>
	</div>
{:else}
	<div class="min-h-screen bg-background text-foreground">
		{@render children()}
	</div>
{/if}
