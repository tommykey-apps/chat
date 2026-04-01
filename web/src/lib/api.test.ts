import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$lib/stores/auth.svelte', () => ({
	getAuthState: vi.fn()
}));

import {
	createRoom,
	listRooms,
	getRoom,
	joinRoom,
	leaveRoom,
	getMessages,
	getUnreadCounts,
	clearUnreadCount
} from './api';
import { getAuthState } from '$lib/stores/auth.svelte';

let mockFetch: ReturnType<typeof vi.fn>;

beforeEach(() => {
	vi.restoreAllMocks();
	mockFetch = vi.fn();
	vi.stubGlobal('fetch', mockFetch);
});

function mockJsonResponse(data: unknown, status = 200) {
	return mockFetch.mockResolvedValue({
		ok: true,
		status,
		json: () => Promise.resolve(data),
		text: () => Promise.resolve(JSON.stringify(data))
	});
}

function mockNoContentResponse() {
	return mockFetch.mockResolvedValue({
		ok: true,
		status: 204,
		json: () => Promise.resolve(undefined),
		text: () => Promise.resolve('')
	});
}

function mockErrorResponse(status: number, body = '') {
	return mockFetch.mockResolvedValue({
		ok: false,
		status,
		statusText: 'Not Found',
		text: () => Promise.resolve(body)
	});
}

describe('api client', () => {
	describe('request internals', () => {
		it('adds Authorization header when token exists', async () => {
			vi.mocked(getAuthState).mockReturnValue({
				user: { sub: 'u1', email: 'a@b.com' },
				token: 'my-jwt-token',
				isAuthenticated: true,
				loading: false
			});
			mockJsonResponse([]);

			await listRooms();

			expect(mockFetch).toHaveBeenCalledWith('/api/rooms', {
				method: 'GET',
				headers: {
					'Content-Type': 'application/json',
					Authorization: 'Bearer my-jwt-token'
				},
				body: undefined
			});
		});

		it('omits Authorization header when no token', async () => {
			vi.mocked(getAuthState).mockReturnValue({
				user: null,
				token: null,
				isAuthenticated: false,
				loading: false
			});
			mockJsonResponse([]);

			await listRooms();

			const headers = mockFetch.mock.calls[0][1].headers;
			expect(headers).not.toHaveProperty('Authorization');
		});

		it('throws on non-ok response with error body', async () => {
			vi.mocked(getAuthState).mockReturnValue({
				user: null, token: 'tok', isAuthenticated: true, loading: false
			});
			mockErrorResponse(404, 'Room not found');

			await expect(listRooms()).rejects.toThrow('Room not found');
		});

		it('throws with status text when error body is empty', async () => {
			vi.mocked(getAuthState).mockReturnValue({
				user: null, token: 'tok', isAuthenticated: true, loading: false
			});
			mockErrorResponse(404);

			await expect(listRooms()).rejects.toThrow('404 Not Found');
		});

		it('handles 204 No Content response', async () => {
			vi.mocked(getAuthState).mockReturnValue({
				user: null, token: 'tok', isAuthenticated: true, loading: false
			});
			mockNoContentResponse();

			const result = await joinRoom('room-1');
			expect(result).toBeUndefined();
		});
	});

	describe('API functions', () => {
		beforeEach(() => {
			vi.mocked(getAuthState).mockReturnValue({
				user: null, token: 'tok', isAuthenticated: true, loading: false
			});
		});

		it('createRoom sends POST with name and description', async () => {
			const room = { id: 'r1', name: 'Test', description: 'Desc', createdBy: 'u1', createdAt: '', memberCount: 1 };
			mockJsonResponse(room);

			const result = await createRoom('Test', 'Desc');

			expect(result).toEqual(room);
			expect(mockFetch).toHaveBeenCalledWith('/api/rooms', expect.objectContaining({
				method: 'POST',
				body: JSON.stringify({ name: 'Test', description: 'Desc' })
			}));
		});

		it('listRooms sends GET to /api/rooms', async () => {
			mockJsonResponse([]);

			const result = await listRooms();

			expect(result).toEqual([]);
			expect(mockFetch).toHaveBeenCalledWith('/api/rooms', expect.objectContaining({
				method: 'GET'
			}));
		});

		it('getRoom sends GET with roomId', async () => {
			const room = { id: 'r1', name: 'Test', description: '', createdBy: 'u1', createdAt: '', memberCount: 1 };
			mockJsonResponse(room);

			const result = await getRoom('r1');

			expect(result).toEqual(room);
			expect(mockFetch).toHaveBeenCalledWith('/api/rooms/r1', expect.objectContaining({ method: 'GET' }));
		});

		it('leaveRoom sends DELETE', async () => {
			mockNoContentResponse();

			await leaveRoom('r1');

			expect(mockFetch).toHaveBeenCalledWith('/api/rooms/r1/leave', expect.objectContaining({ method: 'DELETE' }));
		});

		it('getMessages sends GET with pagination params', async () => {
			const page = { content: [], totalPages: 0, totalElements: 0, number: 0 };
			mockJsonResponse(page);

			await getMessages('r1', 2, 25);

			expect(mockFetch).toHaveBeenCalledWith(
				'/api/rooms/r1/messages?page=2&size=25',
				expect.objectContaining({ method: 'GET' })
			);
		});

		it('getUnreadCounts sends GET to /api/notifications/unread', async () => {
			mockJsonResponse({ 'room-1': 5 });

			const result = await getUnreadCounts();

			expect(result).toEqual({ 'room-1': 5 });
			expect(mockFetch).toHaveBeenCalledWith('/api/notifications/unread', expect.objectContaining({ method: 'GET' }));
		});

		it('clearUnreadCount sends DELETE with roomId', async () => {
			mockNoContentResponse();

			await clearUnreadCount('room-1');

			expect(mockFetch).toHaveBeenCalledWith(
				'/api/notifications/unread/room-1',
				expect.objectContaining({ method: 'DELETE' })
			);
		});
	});
});
