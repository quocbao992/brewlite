import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => authService.getUser());
  const [token, setToken] = useState(() => authService.getToken());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const savedUser = authService.getUser();
    const savedToken = authService.getToken();
    if (savedToken && savedUser) {
      setUser(savedUser);
      setToken(savedToken);
    }
  }, []);

  const login = async (username, password) => {
    setLoading(true);
    try {
      const res = await authApi.login({ username, password });
      const authToken = res.token || res.data?.token;
      const userInfo = res.user || res.data?.user;
      const refreshToken = res.refreshToken || res.data?.refreshToken;

      authService.setToken(authToken);
      authService.setRefreshToken(refreshToken);
      authService.setUser(userInfo);

      setToken(authToken);
      setUser(userInfo);
      return { success: true, data: res };
    } catch (error) {
      return { success: false, error: error.message || 'Đăng nhập thất bại' };
    } finally {
      setLoading(false);
    }
  };

  const register = async (registerData) => {
    setLoading(true);
    try {
      const res = await authApi.register(registerData);
      return { success: true, data: res };
    } catch (error) {
      return { success: false, error: error.message || 'Đăng ký thất bại' };
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      const refreshToken = authService.getRefreshToken();
      await authApi.logout({ refreshToken });
    } catch {} finally {
      authService.clearSession();
      setUser(null);
      setToken(null);
    }
  };

  const value = {
    user,
    token,
    loading,
    isAuthenticated: Boolean(token),
    isAdmin: user?.role === 'ADMIN',
    isBarista: user?.role === 'BARISTA',
    isStaff: user?.role === 'STAFF' || user?.role === 'ADMIN',
    canManageOrders: user?.role === 'ADMIN' || user?.role === 'BARISTA' || user?.role === 'STAFF',
    canAccessAdminPortal: user?.role === 'ADMIN' || user?.role === 'BARISTA',
    login,
    register,
    logout,
    setUser: (u) => {
      authService.setUser(u);
      setUser(u);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
