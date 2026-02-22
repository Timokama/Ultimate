/**
 * Ultimate Defensive Driving School - Authentication Module
 */

// Storage Keys
const TOKEN_KEY = 'ultimate_token';
const USER_KEY = 'ultimate_user';

// API Base URL
const API_BASE = '/api';

const auth = {
    async login(email, password) {
        try {
            const response = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: new URLSearchParams({ email, password })
            });
            const data = await response.json();
            
            if (data.success) {
                localStorage.setItem(TOKEN_KEY, data.token);
                localStorage.setItem(USER_KEY, JSON.stringify(data.user));
                return data.user;
            } else {
                throw new Error(data.message || 'Login failed');
            }
        } catch (error) {
            throw error;
        }
    },
    
    async register(formData) {
        try {
            const response = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: new URLSearchParams(formData)
            });
            const data = await response.json();
            
            if (data.success) {
                localStorage.setItem(TOKEN_KEY, data.token);
                localStorage.setItem(USER_KEY, JSON.stringify(data.user));
                return data.user;
            } else {
                throw new Error(data.message || 'Registration failed');
            }
        } catch (error) {
            console.error('[AUTH] Registration failed:', error.message);
            throw error;
        }
    },
    
    logout() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        window.location.href = 'index.html';
    },
    
    getToken() {
        return localStorage.getItem(TOKEN_KEY);
    },
    
    getUser() {
        const user = localStorage.getItem(USER_KEY);
        return user ? JSON.parse(user) : null;
    },
    
    isLoggedIn() {
        return !!this.getToken();
    }
};
