import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$lib/api', () => ({
	getUnreadCounts: vi.fn(),
	clearUnreadCount: vi.fn()
}));

import {
	getNotificationState,
	setCurrentRoom,
	handleNotification,
	dismissToast,
	clearUnread,
	loadUnreadCounts,
	type NotificationPayload
} from './notifications.svelte';
import { getUnreadCounts, clearUnreadCount } from '$lib/api';

function makePayload(overrides: Partial<NotificationPayload> = {}): NotificationPayload {
	return {
		roomId: 'room-1',
		messageId: 'msg-1',
		senderName: 'Alice',
		contentPreview: 'Hello!',
		messageType: 'TEXT',
		createdAt: '2026-01-01T00:00:00Z',
		...overrides
	};
}

describe('notifications store', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		vi.restoreAllMocks();

		// Reset store state
		setCurrentRoom(null);
		const state = getNotificationState();
		// Clear toasts by dismissing all
		for (const t of [...state.toasts]) {
			dismissToast(t.id);
		}
		// Clear unread counts by calling clearUnread for each key
		// We need to reset the module-level state; simplest way is to clear via exported functions
		for (const roomId of Object.keys(state.unreadCounts)) {
			// clearUnread is async but we just need the sync side-effect
			clearUnread(roomId);
		}
	});

	describe('handleNotification', () => {
		it('adds a toast and increments unread count', () => {
			handleNotification(makePayload());

			const state = getNotificationState();
			expect(state.unreadCounts['room-1']).toBe(1);
			expect(state.toasts).toHaveLength(1);
			expect(state.toasts[0].senderName).toBe('Alice');
			expect(state.toasts[0].contentPreview).toBe('Hello!');
			expect(state.toasts[0].roomId).toBe('room-1');
		});

		it('increments unread count on multiple notifications for same room', () => {
			handleNotification(makePayload({ messageId: 'msg-1' }));
			handleNotification(makePayload({ messageId: 'msg-2' }));
			handleNotification(makePayload({ messageId: 'msg-3' }));

			const state = getNotificationState();
			expect(state.unreadCounts['room-1']).toBe(3);
			expect(state.toasts).toHaveLength(3);
		});

		it('ignores notifications for the current room', () => {
			setCurrentRoom('room-1');
			handleNotification(makePayload({ roomId: 'room-1' }));

			const state = getNotificationState();
			expect(state.unreadCounts['room-1']).toBeUndefined();
			expect(state.toasts).toHaveLength(0);
		});

		it('auto-dismisses toast after 5 seconds', () => {
			handleNotification(makePayload());

			const state = getNotificationState();
			expect(state.toasts).toHaveLength(1);

			vi.advanceTimersByTime(5000);
			expect(state.toasts).toHaveLength(0);
		});

		it('uses crypto.randomUUID when messageId is empty', () => {
			const mockUUID = '00000000-0000-0000-0000-000000000000';
			vi.stubGlobal('crypto', { randomUUID: () => mockUUID });

			handleNotification(makePayload({ messageId: '' }));

			const state = getNotificationState();
			expect(state.toasts[0].id).toBe(mockUUID);
		});
	});

	describe('setCurrentRoom', () => {
		it('sets the current room ID', () => {
			setCurrentRoom('room-42');
			expect(getNotificationState().currentRoomId).toBe('room-42');
		});

		it('can set room to null', () => {
			setCurrentRoom('room-42');
			setCurrentRoom(null);
			expect(getNotificationState().currentRoomId).toBeNull();
		});
	});

	describe('clearUnread', () => {
		it('removes unread count for the room and calls API', async () => {
			vi.mocked(clearUnreadCount).mockResolvedValue(undefined);

			handleNotification(makePayload({ roomId: 'room-1' }));
			expect(getNotificationState().unreadCounts['room-1']).toBe(1);

			await clearUnread('room-1');
			expect(getNotificationState().unreadCounts['room-1']).toBeUndefined();
			expect(clearUnreadCount).toHaveBeenCalledWith('room-1');
		});

		it('removes count even if API call fails', async () => {
			vi.mocked(clearUnreadCount).mockRejectedValue(new Error('network error'));

			handleNotification(makePayload({ roomId: 'room-1' }));
			await clearUnread('room-1');

			expect(getNotificationState().unreadCounts['room-1']).toBeUndefined();
		});
	});

	describe('dismissToast', () => {
		it('removes the toast with matching id', () => {
			handleNotification(makePayload({ messageId: 'msg-a' }));
			handleNotification(makePayload({ messageId: 'msg-b', roomId: 'room-2' }));

			const state = getNotificationState();
			expect(state.toasts).toHaveLength(2);

			dismissToast('msg-a');
			expect(state.toasts).toHaveLength(1);
			expect(state.toasts[0].id).toBe('msg-b');
		});

		it('does nothing if toast id not found', () => {
			handleNotification(makePayload());
			dismissToast('nonexistent');
			expect(getNotificationState().toasts).toHaveLength(1);
		});
	});

	describe('loadUnreadCounts', () => {
		it('loads unread counts from API', async () => {
			vi.mocked(getUnreadCounts).mockResolvedValue({ 'room-1': 5, 'room-2': 3 });

			await loadUnreadCounts();

			const state = getNotificationState();
			expect(state.unreadCounts).toEqual({ 'room-1': 5, 'room-2': 3 });
		});

		it('keeps existing counts if API call fails', async () => {
			handleNotification(makePayload({ roomId: 'room-1' }));
			vi.mocked(getUnreadCounts).mockRejectedValue(new Error('network error'));

			await loadUnreadCounts();

			expect(getNotificationState().unreadCounts['room-1']).toBe(1);
		});
	});
});
