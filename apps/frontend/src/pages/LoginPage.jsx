import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api';

export const LoginPage = ({ onNavigate, initialNotice = '' }) => {
  const { login, isAuthenticated } = useAuth();
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [forgotEmail, setForgotEmail] = useState('');
  const [otpToken, setOtpToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      onNavigate('home');
    }
  }, [isAuthenticated, onNavigate]);

  const switchMode = (nextMode) => {
    setMode(nextMode);
    setErrorMsg('');
    setSuccessMsg('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

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

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    const email = forgotEmail.trim();
    if (!email) {
      setErrorMsg('Vui lòng nhập địa chỉ email đã đăng ký.');
      return;
    }

    setLoading(true);
    try {
      const res = await authApi.forgotPassword({ email });
      const baseMsg = res?.message || 'Mã OTP đã được gửi về email của bạn.';
      const devHint = res?.resetToken
        ? ` (Chưa cấu hình Gmail SMTP, mã OTP thử nghiệm: ${res.resetToken})`
        : '';
      setMode('forgot-reset');
      setSuccessMsg(baseMsg + devHint);
    } catch (err) {
      setErrorMsg(err.message || 'Không thể gửi yêu cầu đặt lại mật khẩu.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!otpToken.trim()) {
      setErrorMsg('Vui lòng nhập mã OTP.');
      return;
    }
    if (!newPassword || newPassword.length < 6) {
      setErrorMsg('Mật khẩu mới phải có ít nhất 6 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMsg('Mật khẩu nhập lại không khớp.');
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword({
        email: forgotEmail.trim(),
        otpToken: otpToken.trim(),
        newPassword,
      });
      setOtpToken('');
      setNewPassword('');
      setConfirmPassword('');
      setMode('login');
      setSuccessMsg('Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.');
    } catch (err) {
      setErrorMsg(err.message || 'Mã OTP không đúng hoặc đã hết hạn.');
    } finally {
      setLoading(false);
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
          <h2 style={{ fontSize: '1.8rem', fontWeight: 800 }}>
            {mode === 'login' ? 'Đăng Nhập' : 'Quên Mật Khẩu'}
          </h2>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: '6px' }}>
            {mode === 'login'
              ? 'Chào mừng bạn quay lại với BrewLite Coffee'
              : mode === 'forgot-email'
                ? 'Nhập email để nhận mã OTP 6 số đặt lại mật khẩu'
                : `Nhập mã OTP đã gửi tới ${forgotEmail}`}
          </p>
        </div>

        {initialNotice && !errorMsg && !successMsg && mode === 'login' && (
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

        {successMsg && (
          <div
            style={{
              padding: '12px',
              background: '#dcfce7',
              color: '#15803d',
              borderRadius: 'var(--radius-md)',
              marginBottom: '20px',
              fontSize: '0.9rem',
              fontWeight: 600,
            }}
          >
            {successMsg}
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

        {mode === 'login' && (
          <>
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
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <label className="form-label" style={{ marginBottom: 0 }}>Mật khẩu</label>
                  <button
                    type="button"
                    onClick={() => switchMode('forgot-email')}
                    style={{
                      background: 'none',
                      border: 'none',
                      padding: 0,
                      color: 'var(--color-accent)',
                      fontSize: '0.85rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    Quên mật khẩu?
                  </button>
                </div>
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
                type="button"
                onClick={() => onNavigate('register')}
                style={{ color: 'var(--color-accent)', fontWeight: 700 }}
              >
                Đăng ký ngay
              </button>
            </div>
          </>
        )}

        {mode === 'forgot-email' && (
          <form noValidate onSubmit={handleSendOtp}>
            <div className="form-group">
              <label className="form-label">Email đăng ký</label>
              <input
                type="email"
                className="form-input"
                placeholder="VD: example@gmail.com"
                value={forgotEmail}
                onChange={(e) => setForgotEmail(e.target.value)}
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '10px' }}
              disabled={loading}
            >
              {loading ? 'Đang gửi OTP...' : 'Gửi mã OTP qua Email'}
            </button>

            <div style={{ textAlign: 'center', marginTop: '18px' }}>
              <button
                type="button"
                onClick={() => switchMode('login')}
                style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', fontWeight: 600 }}
              >
                Quay lại đăng nhập
              </button>
            </div>
          </form>
        )}

        {mode === 'forgot-reset' && (
          <form noValidate onSubmit={handleResetPassword}>
            <div className="form-group">
              <label className="form-label">Mã OTP (6 số)</label>
              <input
                type="text"
                className="form-input"
                placeholder="Nhập mã OTP từ email"
                value={otpToken}
                onChange={(e) => setOtpToken(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Mật khẩu mới</label>
              <input
                type="password"
                className="form-input"
                placeholder="Tối thiểu 6 ký tự"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Nhập lại mật khẩu mới</label>
              <input
                type="password"
                className="form-input"
                placeholder="Nhập lại mật khẩu mới"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '10px' }}
              disabled={loading}
            >
              {loading ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
            </button>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '18px', fontSize: '0.88rem' }}>
              <button
                type="button"
                onClick={() => switchMode('forgot-email')}
                style={{ color: 'var(--color-accent)', fontWeight: 600 }}
              >
                Gửi lại mã OTP
              </button>
              <button
                type="button"
                onClick={() => switchMode('login')}
                style={{ color: 'var(--color-text-secondary)', fontWeight: 600 }}
              >
                Quay lại đăng nhập
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default LoginPage;
