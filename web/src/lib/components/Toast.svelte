<script lang="ts">
	import { goto } from '$app/navigation';
	import { getNotificationState, dismissToast } from '$lib/stores/notifications.svelte';

	const notifications = getNotificationState();
</script>

{#if notifications.toasts.length > 0}
	<div class="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
		{#each notifications.toasts as toast (toast.id)}
			<button
				onclick={() => { dismissToast(toast.id); goto(`/rooms/${toast.roomId}`); }}
				class="w-72 rounded-lg border border-border bg-card p-3 shadow-lg transition hover:bg-muted text-left"
			>
				<p class="text-xs font-medium text-primary">{toast.senderName}</p>
				<p class="mt-1 truncate text-sm text-foreground">{toast.contentPreview}</p>
			</button>
		{/each}
	</div>
{/if}
