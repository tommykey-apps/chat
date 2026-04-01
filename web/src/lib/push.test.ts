import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$lib/api', () => ({
	getVapidKey: vi.fn(),
	subscribePush: vi.fn()
}));

import { initPushNotifications } from './push';
import { getVapidKey, subscribePush } from '$lib/api';

function createMockRegistration(existingSubscription: unknown = null) {
	const mockSubscription = {
		endpoint: 'https://push.example.com/abc',
		toJSON: () => ({
			endpoint: 'https://push.example.com/abc',
			keys: { p256dh: 'p256dh-key', auth: 'auth-key' }
		})
	};

	return {
		pushManager: {
			getSubscription: vi.fn().mockResolvedValue(existingSubscription),
			subscribe: vi.fn().mockResolvedValue(mockSubscription)
		}
	};
}

function setupServiceWorker(reg: ReturnType<typeof createMockRegistration>) {
	const swMock = {
		register: vi.fn().mockResolvedValue(reg),
		ready: Promise.resolve(reg)
	};
	Object.defineProperty(navigator, 'serviceWorker', {
		value: swMock,
		writable: true,
		configurable: true
	});
}

describe('push notifications', () => {
	let mockRegistration: ReturnType<typeof createMockRegistration>;

	beforeEach(() => {
		vi.restoreAllMocks();
		vi.clearAllMocks();
		mockRegistration = createMockRegistration();
		setupServiceWorker(mockRegistration);

		// Mock PushManager
		vi.stubGlobal('PushManager', class {});

		vi.mocked(getVapidKey).mockResolvedValue({ publicKey: 'BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkOs-WLLo-0' });
		vi.mocked(subscribePush).mockResolvedValue(undefined);
	});

	it('skips if serviceWorker is not supported', async () => {
		Object.defineProperty(navigator, 'serviceWorker', {
			value: undefined,
			writable: true,
			configurable: true
		});

		await initPushNotifications();

		expect(getVapidKey).not.toHaveBeenCalled();
	});

	it('skips if PushManager is not supported', async () => {
		// @ts-expect-error - removing PushManager for test
		delete window.PushManager;

		await initPushNotifications();

		expect(getVapidKey).not.toHaveBeenCalled();
	});

	it('registers service worker at /sw.js', async () => {
		await initPushNotifications();

		expect(navigator.serviceWorker.register).toHaveBeenCalledWith('/sw.js');
	});

	it('skips if already subscribed', async () => {
		const existingSub = { endpoint: 'https://existing.com' };
		mockRegistration = createMockRegistration(existingSub);
		setupServiceWorker(mockRegistration);

		await initPushNotifications();

		expect(getVapidKey).not.toHaveBeenCalled();
		expect(subscribePush).not.toHaveBeenCalled();
	});

	it('subscribes and sends keys to backend', async () => {
		await initPushNotifications();

		expect(mockRegistration.pushManager.subscribe).toHaveBeenCalledWith({
			userVisibleOnly: true,
			applicationServerKey: expect.any(Uint8Array)
		});
		expect(subscribePush).toHaveBeenCalledWith(
			'https://push.example.com/abc',
			'p256dh-key',
			'auth-key'
		);
	});

	it('skips backend call if publicKey is empty', async () => {
		mockRegistration = createMockRegistration();
		setupServiceWorker(mockRegistration);
		vi.mocked(getVapidKey).mockResolvedValue({ publicKey: '' });

		await initPushNotifications();

		expect(mockRegistration.pushManager.subscribe).not.toHaveBeenCalled();
		expect(subscribePush).not.toHaveBeenCalled();
	});

	it('skips backend call if subscription keys are missing', async () => {
		const noKeysSub = {
			endpoint: 'https://push.example.com/abc',
			toJSON: () => ({ endpoint: 'https://push.example.com/abc' })
		};
		mockRegistration = createMockRegistration();
		mockRegistration.pushManager.subscribe.mockResolvedValue(noKeysSub);
		setupServiceWorker(mockRegistration);

		await initPushNotifications();

		expect(subscribePush).not.toHaveBeenCalled();
	});

	it('logs error and does not throw on failure', async () => {
		const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
		vi.mocked(getVapidKey).mockRejectedValue(new Error('network'));

		await expect(initPushNotifications()).resolves.toBeUndefined();
		expect(consoleSpy).toHaveBeenCalledWith('Push notification setup failed:', expect.any(Error));
	});
});
