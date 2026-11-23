import { createContext, useContext, useState } from 'react';
import { authApi } from '../api/client';

const AuthContext = createContext(null);

function readUser() {
  try { return JSON.parse(localStorage.getItem('user')); } catch { return null; }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  const saveSession = ({ token, user }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    setUser(user);
    return user;
  };

  const login = async (values) => saveSession(await authApi.login(values));
  const register = async (values) => saveSession(await authApi.register(values));
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const value = { user, isAdmin: user?.role === 'ADMIN', login, register, logout };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
