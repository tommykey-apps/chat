import { getUnreadCounts, clearUnreadCount } from '$lib/api';

export interface NotificationPayload {
	roomId: string;
	messageId: string;
	senderName: string;
	contentPreview: string;
	messageType: string;
	createdAt: string;
}

export interface Toast {
	id: string;
	roomId: string;
	senderName: string;
	contentPreview: string;
	timestamp: number;
}

let currentRoomId = $state<string | null>(null);
let unreadCounts = $state<Record<string, number>>({});
let toasts = $state<Toast[]>([]);

export function getNotificationState() {
	return {
		get currentRoomId() { return currentRoomId; },
		get unreadCounts() { return unreadCounts; },
		get toasts() { return toasts; }
	};
}

export function setCurrentRoom(roomId: string | null) {
	currentRoomId = roomId;
}

export function handleNotification(payload: NotificationPayload) {
	if (payload.roomId === currentRoomId) return;

	unreadCounts = {
		...unreadCounts,
		[payload.roomId]: (unreadCounts[payload.roomId] || 0) + 1
	};

	const toast: Toast = {
		id: payload.messageId || crypto.randomUUID(),
		roomId: payload.roomId,
		senderName: payload.senderName,
		contentPreview: payload.contentPreview,
		timestamp: Date.now()
	};
	toasts = [...toasts, toast];

	setTimeout(() => {
		dismissToast(toast.id);
	}, 5000);
}

export function dismissToast(id: string) {
	toasts = toasts.filter((t) => t.id !== id);
}

export async function clearUnread(roomId: string) {
	const { [roomId]: _, ...rest } = unreadCounts;
	unreadCounts = rest;
	try {
		await clearUnreadCount(roomId);
	} catch {
		// ignore
	}
}

export async function loadUnreadCounts() {
	try {
		unreadCounts = await getUnreadCounts();
	} catch {
		// ignore
	}
}
