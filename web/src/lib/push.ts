import { getVapidKey, subscribePush } from '$lib/api';

export async function initPushNotifications(): Promise<void> {
	if (!('serviceWorker' in navigator) || !('PushManager' in window)) return;

	try {
		const registration = await navigator.serviceWorker.register('/sw.js');
		await navigator.serviceWorker.ready;

		const existing = await registration.pushManager.getSubscription();
		if (existing) return;

		const { publicKey } = await getVapidKey();
		if (!publicKey) return;

		const applicationServerKey = urlBase64ToUint8Array(publicKey);
		const subscription = await registration.pushManager.subscribe({
			userVisibleOnly: true,
			applicationServerKey
		});

		const json = subscription.toJSON();
		if (json.endpoint && json.keys?.p256dh && json.keys?.auth) {
			await subscribePush(json.endpoint, json.keys.p256dh, json.keys.auth);
		}
	} catch (e) {
		console.error('Push notification setup failed:', e);
	}
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
	const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
	const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
	const rawData = atob(base64);
	return Uint8Array.from([...rawData].map((c) => c.charCodeAt(0)));
}
