import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	define: {
		global: 'globalThis'
	},
	plugins: [tailwindcss(), sveltekit()],
	server: {
		proxy: {
			'/api': 'http://localhost:8080',
			'/ws': {
				target: 'http://localhost:8080',
				ws: true
			}
		}
	}
});
