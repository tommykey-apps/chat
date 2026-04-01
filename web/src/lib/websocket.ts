import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { getAuthState } from '$lib/stores/auth.svelte';

let client: Client | null = null;
let connected = $state(false);
let onConnectCallbacks: Array<() => void> = [];

export function getConnected() {
	return {
		get value() { return connected; }
	};
}

export function connect(onConnect?: () => void) {
	if (onConnect) onConnectCallbacks.push(onConnect);
	if (client?.connected) {
		onConnect?.();
		return;
	}
	if (client?.active) return;

	const auth = getAuthState();
	const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
	const wsUrl = `${protocol}//${window.location.host}/ws`;

	client = new Client({
		brokerURL: wsUrl,
		connectHeaders: {
			Authorization: `Bearer ${auth.token || ''}`
		},
		reconnectDelay: 5000,
		heartbeatIncoming: 10000,
		heartbeatOutgoing: 10000,
		onConnect: () => {
			connected = true;
			onConnectCallbacks.forEach((cb) => cb());
		},
		onDisconnect: () => {
			connected = false;
		},
		onStompError: (frame) => {
			console.error('STOMP error:', frame.headers['message']);
		}
	});

	client.activate();
}

export function subscribe(destination: string, callback: (msg: IMessage) => void): StompSubscription | null {
	if (!client?.connected) return null;
	return client.subscribe(destination, callback);
}

export function send(destination: string, body: object) {
	if (!client?.connected) return;
	client.publish({ destination, body: JSON.stringify(body) });
}

export function disconnect() {
	if (client) {
		client.deactivate();
		client = null;
		connected = false;
		onConnectCallbacks = [];
	}
}

export function isConnected() {
	return client?.connected ?? false;
}
