import { test, expect } from '@playwright/test';

test.describe('Smoke tests', () => {
	test('login page renders with form elements', async ({ page }) => {
		await page.goto('/login');

		// Title
		await expect(page.locator('text=Chat')).toBeVisible();

		// Login tab and signup tab
		await expect(page.getByRole('tab', { name: 'ログイン' })).toBeVisible();
		await expect(page.getByRole('tab', { name: 'サインアップ' })).toBeVisible();

		// Email and password inputs
		await expect(page.getByPlaceholder('メールアドレス')).toBeVisible();
		await expect(page.getByPlaceholder('パスワード')).toBeVisible();

		// Submit button
		await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible();
	});

	test('login page has correct page title', async ({ page }) => {
		await page.goto('/login');
		await expect(page).toHaveTitle('Chat');
	});

	test('unauthenticated user is redirected to /login from root', async ({ page }) => {
		await page.goto('/');
		await page.waitForURL('/login');
		expect(page.url()).toContain('/login');
	});

	test('unauthenticated user is redirected to /login from /rooms', async ({ page }) => {
		await page.goto('/rooms');
		await page.waitForURL('/login');
		expect(page.url()).toContain('/login');
	});

	test('unauthenticated user is redirected to /login from /friends', async ({ page }) => {
		await page.goto('/friends');
		await page.waitForURL('/login');
		expect(page.url()).toContain('/login');
	});

	test('login page signup tab switches form button text', async ({ page }) => {
		await page.goto('/login');

		await page.getByRole('tab', { name: 'サインアップ' }).click();
		await expect(page.getByRole('button', { name: 'サインアップ' })).toBeVisible();

		await page.getByRole('tab', { name: 'ログイン' }).click();
		await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible();
	});

	test('favicon loads successfully', async ({ page }) => {
		await page.goto('/login');
		const favicon = page.locator('link[rel="icon"]');
		const href = await favicon.getAttribute('href');
		expect(href).toBeTruthy();

		if (href) {
			const response = await page.request.get(href);
			expect(response.ok()).toBe(true);
		}
	});

	test('dark mode class is applied to html element', async ({ page }) => {
		await page.goto('/login');
		await expect(page.locator('html')).toHaveClass(/dark/);
	});
});
