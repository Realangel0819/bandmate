import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  userId: number | null;
  email: string | null;
  nickname: string | null;
  token: string | null;
  setAuth: (userId: number, email: string, nickname: string, token: string) => void;
  logout: () => void;
  isLoggedIn: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      userId: null,
      email: null,
      nickname: null,
      token: null,
      setAuth: (userId, email, nickname, token) => {
        localStorage.setItem('token', token);
        set({ userId, email, nickname, token });
      },
      logout: () => {
        localStorage.removeItem('token');
        set({ userId: null, email: null, nickname: null, token: null });
      },
      isLoggedIn: () => get().token !== null,
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        userId: state.userId,
        email: state.email,
        nickname: state.nickname,
        token: state.token,
      }),
    }
  )
);