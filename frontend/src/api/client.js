import axios from 'axios';

export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const api = axios.create({ baseURL: `${API_URL}/api` });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && localStorage.getItem('token')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export function errorMessage(err) {
  const data = err?.response?.data;
  if (data?.fieldErrors) return Object.values(data.fieldErrors).join(', ');
  return data?.message || err?.message || 'Something went wrong';
}

const d = (r) => r.data;

export const authApi = {
  login: (body) => api.post('/auth/login', body).then(d),
  register: (body) => api.post('/auth/register', body).then(d),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }).then(d),
  resetPassword: (token, newPassword) => api.post('/auth/reset-password', { token, newPassword }).then(d),
};

export const quizApi = {
  list: () => api.get('/quizzes').then(d),
  start: (quizId) => api.post(`/quizzes/${quizId}/start`).then(d),
  submit: (attemptId, answers) => api.post(`/attempts/${attemptId}/submit`, { answers }).then(d),
  myResults: () => api.get('/attempts/my').then(d),
  result: (attemptId) => api.get(`/attempts/${attemptId}`).then(d),
};

export const adminApi = {
  quizzes: () => api.get('/admin/quizzes').then(d),
  quiz: (id) => api.get(`/admin/quizzes/${id}`).then(d),
  createQuiz: (body) => api.post('/admin/quizzes', body).then(d),
  updateQuiz: (id, body) => api.put(`/admin/quizzes/${id}`, body).then(d),
  deleteQuiz: (id) => api.delete(`/admin/quizzes/${id}`),
  addQuestion: (quizId, body) => api.post(`/admin/quizzes/${quizId}/questions`, body).then(d),
  updateQuestion: (id, body) => api.put(`/admin/questions/${id}`, body).then(d),
  deleteQuestion: (id) => api.delete(`/admin/questions/${id}`),
  results: (quizId) => api.get('/admin/results', { params: quizId ? { quizId } : {} }).then(d),
  users: () => api.get('/admin/users').then(d),
  updateUser: (id, body) => api.put(`/admin/users/${id}`, body).then(d),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
};

export default api;
