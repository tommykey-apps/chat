import { getSession, logout as cognitoLogout } from '$lib/auth';

let user = $state<{ sub: string; email: string } | null>(null);
let token = $state<string | null>(null);
let isAuthenticated = $state(false);
let loading = $state(true);

export function getAuthState() {
	return {
		get user() { return user; },
		get token() { return token; },
		get isAuthenticated() { return isAuthenticated; },
		get loading() { return loading; }
	};
}

export function setAuth(u: { sub: string; email: string }, t: string) {
	user = u;
	token = t;
	isAuthenticated = true;
	loading = false;
}

export function clearAuth() {
	user = null;
	token = null;
	isAuthenticated = false;
	loading = false;
}

export async function initAuth() {
	if (import.meta.env.DEV) {
		setAuth({ sub: 'dev-user', email: 'dev@local' }, 'dev-token');
		return;
	}
	try {
		const session = await getSession();
		if (session) {
			const idToken = session.getIdToken();
			setAuth(
				{ sub: idToken.payload.sub!, email: idToken.payload.email as string },
				session.getAccessToken().getJwtToken()
			);
		} else {
			clearAuth();
		}
	} catch {
		clearAuth();
	}
}

export function logout() {
	cognitoLogout();
	clearAuth();
}
