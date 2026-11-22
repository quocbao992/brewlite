import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api';
import { OtpVerificationModal, formatCountdown } from '../components/common/OtpVerificationModal';

export const LoginPage = ({ onNavigate, initialNotice = '' }) => {
  const { login, isAuthenticated } = useAuth();
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [forgotEmail, setForgotEmail] = useState('');
  const [verifiedEmail, setVerifiedEmail] = useState('');
  const [otpToken, setOtpToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const [otpModalOpen, setOtpModalOpen] = useState(false);
  const [otpModalLoading, setOtpModalLoading] = useState(false);
  const [otpResending, setOtpResending] = useState(false);
  const [otpModalError, setOtpModalError] = useState('');
  const [devOtpHint, setDevOtpHint] = useState('');
  const [otpExpiresAt, setOtpExpiresAt] = useState(null);
  const [resendAvailableAt, setResendAvailableAt] = useState(null);
  const [nowMs, setNowMs] = useState(() => Date.now());

  useEffect(() => {
    if (isAuthenticated) {
      onNavigate('home');
    }
  }, [isAuthenticated, onNavigate]);

  useEffect(() => {
    const hasPendingTimers =
      (otpExpiresAt && otpExpiresAt > Date.now()) ||
      (resendAvailableAt && resendAvailableAt > Date.now());
    if (!hasPendingTimers) return undefined;

    setNowMs(Date.now());
    const intervalId = setInterval(() => {
      const current = Date.now();
      setNowMs(current);
      if (
        (!otpExpiresAt || current >= otpExpiresAt) &&
        (!resendAvailableAt || current >= resendAvailableAt)
      ) {
        clearInterval(intervalId);
      }
    }, 1000);

    return () => clearInterval(intervalId);
  }, [otpExpiresAt, resendAvailableAt]);

  const otpRemainingSeconds = otpExpiresAt
    ? Math.max(0, Math.ceil((otpExpiresAt - nowMs) / 1000))
    : 0;
  const resendRemainingSeconds = resendAvailableAt
    ? Math.max(0, Math.ceil((resendAvailableAt - nowMs) / 1000))
    : 0;

  const normalizedForgotEmail = forgotEmail.trim().toLowerCase();
  const isSameSentEmail = Boolean(verifiedEmail && verifiedEmail === normalizedForgotEmail);
  const hasActiveOtpForCurrentEmail = isSameSentEmail && otpRemainingSeconds > 0;
  const isCooldownActiveForCurrentEmail = isSameSentEmail && resendRemainingSeconds > 0;

  const switchMode = (nextMode) => {
    setMode(nextMode);
    setErrorMsg('');
    setSuccessMsg('');
    setOtpModalOpen(false);
    setOtpModalError('');
  };

  const applyOtpResponse = (emailLower, res) => {
    const now = Date.now();
    const ttlSeconds = Number(res?.expiresInSeconds) || 900;
    const cooldownSeconds = Number(res?.resendAfterSeconds) || 300;
    setVerifiedEmail(emailLower);
    setOtpExpiresAt(now + ttlSeconds * 1000);
    setResendAvailableAt(now + cooldownSeconds * 1000);
    setNowMs(now);
    setDevOtpHint(
      res?.resetToken
        ? `Chưa cấu hình dịch vụ gửi mail, mã OTP thử nghiệm: ${res.resetToken}`
        : ''
    );
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

    if (isCooldownActiveForCurrentEmail && hasActiveOtpForCurrentEmail) {
      setOtpModalError('');
      setOtpModalOpen(true);
      return;
    }

    setLoading(true);
    try {
      const res = await authApi.forgotPassword({ email });
      setOtpToken('');
      applyOtpResponse(email.toLowerCase(), res);
      setOtpModalError('');
      setOtpModalOpen(true);
    } catch (err) {
      setErrorMsg(err.message || 'Không thể gửi yêu cầu đặt lại mật khẩu.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (submittedOtp) => {
    setOtpModalError('');
    setOtpModalLoading(true);
    try {
      await authApi.verifyOtp({
        email: verifiedEmail,
        otpToken: submittedOtp,
      });
      setOtpToken(submittedOtp);
      setOtpModalOpen(false);
      setNewPassword('');
      setConfirmPassword('');
      setErrorMsg('');
      setMode('forgot-reset');
      setSuccessMsg('Xác thực OTP thành công! Vui lòng đặt lại mật khẩu mới.');
    } catch (err) {
      setOtpModalError(err.message || 'Mã OTP không chính xác hoặc đã hết hạn.');
    } finally {
      setOtpModalLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!verifiedEmail || resendRemainingSeconds > 0) return;
    setOtpModalError('');
    setOtpResending(true);
    try {
      const res = await authApi.forgotPassword({ email: verifiedEmail });
      setOtpToken('');
      applyOtpResponse(verifiedEmail, res);
    } catch (err) {
      setOtpModalError(err.message || 'Không thể gửi lại mã OTP lúc này.');
    } finally {
      setOtpResending(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!otpToken.trim() || otpRemainingSeconds <= 0) {
      setErrorMsg('Phiên xác thực OTP đã hết hạn. Vui lòng xác thực lại mã OTP.');
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
        email: verifiedEmail || forgotEmail.trim(),
        otpToken: otpToken.trim(),
        newPassword,
      });
      setOtpToken('');
      setVerifiedEmail('');
      setOtpExpiresAt(null);
      setResendAvailableAt(null);
      setNewPassword('');
      setConfirmPassword('');
      setMode('login');
      setSuccessMsg('Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.');
    } catch (err) {
      setErrorMsg(err.message || 'Không thể đặt lại mật khẩu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleReopenOtp = () => {
    setErrorMsg('');
    setSuccessMsg('');
    setOtpModalError('');
    if (verifiedEmail && !forgotEmail.trim()) {
      setForgotEmail(verifiedEmail);
    }
    setOtpModalOpen(true);
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
            {mode === 'login'
              ? 'Đăng Nhập'
              : mode === 'forgot-reset'
                ? 'Đặt Lại Mật Khẩu'
                : 'Quên Mật Khẩu'}
          </h2>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: '6px' }}>
            {mode === 'login'
              ? 'Chào mừng bạn quay lại với BrewLite Coffee'
              : mode === 'forgot-email'
                ? 'Nhập email để nhận mã OTP 6 số đặt lại mật khẩu'
                : `Tạo mật khẩu mới cho ${verifiedEmail || forgotEmail}`}
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
                  placeholder="Nhập tên đăng nhập"
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
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
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
                placeholder="Nhập email đã đăng ký"
                value={forgotEmail}
                onChange={(e) => setForgotEmail(e.target.value)}
              />
            </div>

            {isCooldownActiveForCurrentEmail && (
              <div
                style={{
                  padding: '10px 12px',
                  background: '#fef7ed',
                  color: 'var(--color-primary-dark)',
                  border: '1px solid #f3d6b6',
                  borderRadius: 'var(--radius-md)',
                  marginBottom: '14px',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                }}
              >
                Đã gửi OTP tới email này. Có thể gửi lại mã mới sau{' '}
                <strong style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {formatCountdown(resendRemainingSeconds)}
                </strong>
                .
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '10px' }}
              disabled={loading || (isCooldownActiveForCurrentEmail && !hasActiveOtpForCurrentEmail)}
            >
              {loading
                ? 'Đang gửi OTP...'
                : isCooldownActiveForCurrentEmail && hasActiveOtpForCurrentEmail
                  ? 'Mở lại ô nhập mã OTP'
                  : isCooldownActiveForCurrentEmail
                    ? `Gửi lại sau ${formatCountdown(resendRemainingSeconds)}`
                    : 'Gửi mã OTP qua Email'}
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
            <div
              style={{
                padding: '10px 14px',
                background: otpRemainingSeconds > 0 ? 'var(--color-surface-muted)' : '#fee2e2',
                color: otpRemainingSeconds > 0 ? 'var(--color-primary-dark)' : '#b91c1c',
                border: `1px solid ${otpRemainingSeconds > 0 ? 'var(--color-border)' : '#fecaca'}`,
                borderRadius: 'var(--radius-md)',
                marginBottom: '18px',
                fontSize: '0.86rem',
                fontWeight: 600,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              {otpRemainingSeconds > 0 ? (
                <>
                  <span>Phiên OTP hợp lệ</span>
                  <strong style={{ fontVariantNumeric: 'tabular-nums' }}>
                    {formatCountdown(otpRemainingSeconds)}
                  </strong>
                </>
              ) : (
                <span>Phiên OTP đã hết hạn</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Mật khẩu mới</label>
              <input
                type="password"
                className="form-input"
                placeholder="Tối thiểu 6 ký tự"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                disabled={otpRemainingSeconds <= 0}
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
                disabled={otpRemainingSeconds <= 0}
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{
                width: '100%',
                padding: '12px',
                marginTop: '10px',
                opacity: otpRemainingSeconds <= 0 ? 0.55 : 1,
                cursor: otpRemainingSeconds <= 0 ? 'not-allowed' : 'pointer',
              }}
              disabled={loading || otpRemainingSeconds <= 0}
            >
              {loading ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
            </button>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '18px', fontSize: '0.88rem' }}>
              <button
                type="button"
                onClick={handleReopenOtp}
                style={{ color: 'var(--color-accent)', fontWeight: 600 }}
              >
                Nhập / gửi lại OTP
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

      <OtpVerificationModal
        isOpen={otpModalOpen}
        email={verifiedEmail || forgotEmail.trim()}
        otpRemainingSeconds={otpRemainingSeconds}
        resendRemainingSeconds={resendRemainingSeconds}
        loading={otpModalLoading}
        resending={otpResending}
        errorMsg={otpModalError}
        devOtpHint={devOtpHint}
        onClose={() => {
          setOtpModalOpen(false);
          setOtpModalError('');
        }}
        onVerify={handleVerifyOtp}
        onResend={handleResendOtp}
      />
    </div>
  );
};

export default LoginPage;
