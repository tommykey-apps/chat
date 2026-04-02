import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { getAuthState } from '$lib/stores/auth.svelte';

let client: Client | null = null;
let connected = $state(false);
let onConnectCallbacks: Array<() => void> = [];

const INITIAL_RECONNECT_DELAY = 5000;
const MAX_RECONNECT_DELAY = 60000;
let currentReconnectDelay = INITIAL_RECONNECT_DELAY;

export function getWsState() {
	return {
		get connected() { return connected; }
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
		reconnectDelay: currentReconnectDelay,
		heartbeatIncoming: 10000,
		heartbeatOutgoing: 10000,
		onConnect: () => {
			connected = true;
			currentReconnectDelay = INITIAL_RECONNECT_DELAY;
			if (client) client.reconnectDelay = currentReconnectDelay;
			onConnectCallbacks.forEach((cb) => cb());
		},
		onDisconnect: () => {
			connected = false;
		},
		onStompError: (frame) => {
			console.warn('STOMP error:', frame.headers['message']);
			currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY);
			if (client) client.reconnectDelay = currentReconnectDelay;
		},
		onWebSocketError: () => {
			currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY);
			if (client) client.reconnectDelay = currentReconnectDelay;
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
		currentReconnectDelay = INITIAL_RECONNECT_DELAY;
	}
}

export function isConnected() {
	return client?.connected ?? false;
}
