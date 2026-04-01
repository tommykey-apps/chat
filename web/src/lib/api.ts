import { getAuthState } from '$lib/stores/auth.svelte';

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
	const auth = getAuthState();
	const headers: Record<string, string> = { 'Content-Type': 'application/json' };
	if (auth.token) headers['Authorization'] = `Bearer ${auth.token}`;

	const res = await fetch(path, {
		method,
		headers,
		body: body ? JSON.stringify(body) : undefined
	});
	if (!res.ok) {
		const err = await res.text();
		throw new Error(err || `${res.status} ${res.statusText}`);
	}
	if (res.status === 204) return undefined as T;
	return res.json();
}

export interface Room {
	id: string;
	name: string;
	description: string;
	createdBy: string;
	createdAt: string;
	memberCount: number;
}

export interface Message {
	id: string;
	senderId: string;
	senderName: string;
	content: string;
	messageType: string;
	createdAt: string;
}

export interface PageResponse<T> {
	content: T[];
	totalPages: number;
	totalElements: number;
	number: number;
}

export const createRoom = (name: string, description: string) =>
	request<Room>('POST', '/api/rooms', { name, description });

export const listRooms = () =>
	request<Room[]>('GET', '/api/rooms');

export const getRoom = (roomId: string) =>
	request<Room>('GET', `/api/rooms/${roomId}`);

export const joinRoom = (roomId: string) =>
	request<void>('POST', `/api/rooms/${roomId}/join`);

export const leaveRoom = (roomId: string) =>
	request<void>('DELETE', `/api/rooms/${roomId}/leave`);

export const getMessages = (roomId: string, page = 0, size = 50) =>
	request<PageResponse<Message>>('GET', `/api/rooms/${roomId}/messages?page=${page}&size=${size}`);

export const searchMessages = (roomId: string, query: string, page = 0, size = 50) =>
	request<PageResponse<Message>>('GET', `/api/rooms/${roomId}/messages/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);

export const getUploadUrl = (roomId: string, fileName: string, contentType: string) =>
	request<{ uploadUrl: string; s3Key: string }>('POST', '/api/files/presign-upload', { roomId, fileName, contentType });

export const getDownloadUrl = (s3Key: string) =>
	request<{ downloadUrl: string }>('GET', `/api/files/presign-download/${s3Key}`);

// Users
export interface UserInfo {
	id: string;
	email: string;
	displayName: string;
	createdAt: string;
}

export const getMe = () => request<UserInfo>('GET', '/api/users/me');

export const searchUsers = (q: string) =>
	request<UserInfo[]>('GET', `/api/users/search?q=${encodeURIComponent(q)}`);

// Friends
export interface FriendRequest {
	userId: string;
	email: string;
	displayName: string;
	createdAt: string;
}

export const listFriends = () => request<UserInfo[]>('GET', '/api/friends');

export const listFriendRequests = () => request<FriendRequest[]>('GET', '/api/friends/requests');

export const sendFriendRequest = (userId: string) =>
	request<void>('POST', `/api/friends/${userId}/request`);

export const acceptFriendRequest = (userId: string) =>
	request<void>('POST', `/api/friends/${userId}/accept`);

export const removeFriend = (userId: string) =>
	request<void>('DELETE', `/api/friends/${userId}`);

// Notifications
export const getUnreadCounts = () =>
	request<Record<string, number>>('GET', '/api/notifications/unread');

export const clearUnreadCount = (roomId: string) =>
	request<void>('DELETE', `/api/notifications/unread/${roomId}`);

// Push
export const getVapidKey = () =>
	request<{ publicKey: string }>('GET', '/api/push/vapid-key');

export const subscribePush = (endpoint: string, p256dh: string, auth: string) =>
	request<void>('POST', '/api/push/subscribe', { endpoint, p256dh, auth });

export const unsubscribePush = (endpoint: string) =>
	request<void>('DELETE', '/api/push/unsubscribe', { endpoint });
