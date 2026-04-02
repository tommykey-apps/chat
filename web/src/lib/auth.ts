import {
	CognitoUserPool,
	CognitoUser,
	AuthenticationDetails,
	CognitoUserAttribute,
	type CognitoUserSession
} from 'amazon-cognito-identity-js';

const POOL_ID = import.meta.env.VITE_COGNITO_USER_POOL_ID || '';
const CLIENT_ID = import.meta.env.VITE_COGNITO_CLIENT_ID || '';

const userPool = new CognitoUserPool({
	UserPoolId: POOL_ID,
	ClientId: CLIENT_ID
});

export function signUp(email: string, password: string): Promise<string> {
	return new Promise((resolve, reject) => {
		const attributes = [
			new CognitoUserAttribute({ Name: 'email', Value: email })
		];
		userPool.signUp(email, password, attributes, [], (err, result) => {
			if (err) return reject(err);
			resolve(result!.userSub);
		});
	});
}

export function confirmSignUp(email: string, code: string): Promise<void> {
	return new Promise((resolve, reject) => {
		const cognitoUser = new CognitoUser({ Username: email, Pool: userPool });
		cognitoUser.confirmRegistration(code, true, (err) => {
			if (err) return reject(err);
			resolve();
		});
	});
}

export function login(email: string, password: string): Promise<CognitoUserSession> {
	return new Promise((resolve, reject) => {
		const cognitoUser = new CognitoUser({ Username: email, Pool: userPool });
		const authDetails = new AuthenticationDetails({ Username: email, Password: password });
		cognitoUser.authenticateUser(authDetails, {
			onSuccess: (session) => resolve(session),
			onFailure: (err) => reject(err)
		});
	});
}

export function getSession(): Promise<CognitoUserSession | null> {
	return new Promise((resolve) => {
		const cognitoUser = userPool.getCurrentUser();
		if (!cognitoUser) return resolve(null);
		cognitoUser.getSession((err: Error | null, session: CognitoUserSession | null) => {
			if (err || !session?.isValid()) return resolve(null);
			resolve(session);
		});
	});
}

export function forgotPassword(email: string): Promise<void> {
	return new Promise((resolve, reject) => {
		const cognitoUser = new CognitoUser({ Username: email, Pool: userPool });
		cognitoUser.forgotPassword({
			onSuccess: () => resolve(),
			onFailure: (err) => reject(err)
		});
	});
}

export function confirmNewPassword(email: string, code: string, newPassword: string): Promise<void> {
	return new Promise((resolve, reject) => {
		const cognitoUser = new CognitoUser({ Username: email, Pool: userPool });
		cognitoUser.confirmPassword(code, newPassword, {
			onSuccess: () => resolve(),
			onFailure: (err) => reject(err)
		});
	});
}

export function logout(): void {
	const cognitoUser = userPool.getCurrentUser();
	if (cognitoUser) cognitoUser.signOut();
}
