import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';

export const LoginPage = ({ onNavigate, initialNotice = '' }) => {
  const { login, isAuthenticated } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      onNavigate('home');
    }
  }, [isAuthenticated, onNavigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!username.trim()) {
      setErrorMsg('Vui lòng nhập tên đăng nhập.');
      return;
    }
    if (!password) {
      setErrorMsg('Vui lòng nhập mật khẩu.');
      return;
    }

    setLoading(true);

    const result = await login(username, password);
    setLoading(false);

    if (result.success) {
      onNavigate('home');
    } else {
      setErrorMsg(result.error);
    }
  };

  return (
    <div
      className="container"
      style={{
        padding: '60px 24px',
        maxWidth: '480px',
        margin: '0 auto',
      }}
    >
      <div
        style={{
          background: 'var(--color-surface)',
          padding: '36px',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-md)',
          border: '1px solid var(--color-border)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <h2 style={{ fontSize: '1.8rem', fontWeight: 800 }}>Đăng Nhập</h2>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: '6px' }}>
            Chào mừng bạn quay lại với BrewLite Coffee
          </p>
        </div>

        {initialNotice && !errorMsg && (
          <div
            style={{
              padding: '12px 14px',
              background: '#f8fafc',
              color: 'var(--color-text-main)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              marginBottom: '20px',
              fontSize: '0.92rem',
            }}
          >
            {initialNotice}
          </div>
        )}

        {errorMsg && (
          <div
            style={{
              padding: '12px',
              background: '#fee2e2',
              color: '#b91c1c',
              borderRadius: 'var(--radius-md)',
              marginBottom: '20px',
              fontSize: '0.9rem',
              fontWeight: 600,
            }}
          >
            {errorMsg}
          </div>
        )}

        <form noValidate onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Tên đăng nhập</label>
            <input
              type="text"
              className="form-input"
              placeholder="Nhập username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Mật khẩu</label>
            <input
              type="password"
              className="form-input"
              placeholder="Nhập mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '12px', marginTop: '10px' }}
            disabled={loading}
          >
            {loading ? 'Đang xử lý...' : 'Đăng nhập'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.92rem' }}>
          Chưa có tài khoản?{' '}
          <button
            onClick={() => onNavigate('register')}
            style={{ color: 'var(--color-accent)', fontWeight: 700 }}
          >
            Đăng ký ngay
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;

