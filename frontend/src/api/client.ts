import axios from 'axios';

const client = axios.create({
  baseURL: '/api',
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      const msg = err.response?.data?.message;
      return Promise.reject(new Error(msg || '권한이 없습니다.'));
    }
    const msg = err.response?.data?.message || err.message;
    return Promise.reject(new Error(msg));
  }
);

export default client;