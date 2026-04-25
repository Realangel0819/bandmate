import client from './client';

export interface LoginResponse {
  userId: number;
  email: string;
  nickname: string;
  token: string;
}

export const signup = (email: string, password: string, nickname: string) =>
  client.post<string>('/users/signup', { email, password, nickname });

export const login = (email: string, password: string) =>
  client.post<LoginResponse>('/users/login', { email, password });