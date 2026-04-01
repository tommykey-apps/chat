self.addEventListener('push', (event) => {
	const data = event.data?.json() ?? {};
	const title = data.senderName ?? 'New message';
	const options = {
		body: data.contentPreview ?? '',
		tag: `room-${data.roomId}`,
		renotify: true,
		data: { roomId: data.roomId }
	};

	event.waitUntil(
		clients
			.matchAll({ type: 'window', includeUncontrolled: true })
			.then((windowClients) => {
				const isRoomOpen = windowClients.some(
					(c) => c.visibilityState === 'visible' && c.url.includes(`/rooms/${data.roomId}`)
				);
				if (isRoomOpen) return;
				return self.registration.showNotification(title, options);
			})
	);
});

self.addEventListener('notificationclick', (event) => {
	event.notification.close();
	const roomId = event.notification.data?.roomId;
	const url = roomId ? `/rooms/${roomId}` : '/rooms';

	event.waitUntil(
		clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
			for (const client of windowClients) {
				if ('focus' in client) {
					client.navigate(url);
					return client.focus();
				}
			}
			return clients.openWindow(url);
		})
	);
});
