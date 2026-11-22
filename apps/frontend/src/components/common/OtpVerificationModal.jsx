import React, { useEffect, useRef, useState } from 'react';

const OTP_LENGTH = 6;

export const formatCountdown = (totalSeconds) => {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
};

export const OtpVerificationModal = ({
  isOpen,
  email,
  otpRemainingSeconds,
  resendRemainingSeconds,
  loading,
  resending,
  errorMsg,
  devOtpHint,
  onClose,
  onVerify,
  onResend,
}) => {
  const [digits, setDigits] = useState(Array(OTP_LENGTH).fill(''));
  const [focusedIndex, setFocusedIndex] = useState(0);
  const inputRefs = useRef([]);

  useEffect(() => {
    if (isOpen) {
      setDigits(Array(OTP_LENGTH).fill(''));
      setFocusedIndex(0);
      const timer = setTimeout(() => {
        inputRefs.current[0]?.focus();
      }, 60);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const otpValue = digits.join('');
  const isExpired = otpRemainingSeconds <= 0;
  const isUrgent = otpRemainingSeconds > 0 && otpRemainingSeconds <= 60;
  const canSubmit = otpValue.length === OTP_LENGTH && !isExpired && !loading;

  const handleDigitChange = (index, rawValue) => {
    const cleaned = rawValue.replace(/\D/g, '');
    if (!cleaned) {
      const next = [...digits];
      next[index] = '';
      setDigits(next);
      return;
    }

    if (cleaned.length > 1) {
      const next = [...digits];
      let targetIdx = index;
      for (const ch of cleaned) {
        if (targetIdx >= OTP_LENGTH) break;
        next[targetIdx] = ch;
        targetIdx += 1;
      }
      setDigits(next);
      const nextFocus = Math.min(targetIdx, OTP_LENGTH - 1);
      inputRefs.current[nextFocus]?.focus();
      return;
    }

    const next = [...digits];
    next[index] = cleaned;
    setDigits(next);
    if (index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace') {
      if (digits[index]) {
        const next = [...digits];
        next[index] = '';
        setDigits(next);
      } else if (index > 0) {
        const next = [...digits];
        next[index - 1] = '';
        setDigits(next);
        inputRefs.current[index - 1]?.focus();
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      e.preventDefault();
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < OTP_LENGTH - 1) {
      e.preventDefault();
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = (e.clipboardData?.getData('text') || '').replace(/\D/g, '').slice(0, OTP_LENGTH);
    if (!pasted) return;
    const next = Array(OTP_LENGTH).fill('');
    pasted.split('').forEach((ch, idx) => {
      next[idx] = ch;
    });
    setDigits(next);
    const focusIdx = Math.min(pasted.length, OTP_LENGTH) - 1;
    if (focusIdx >= 0) {
      inputRefs.current[focusIdx]?.focus();
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!canSubmit) return;
    onVerify(otpValue);
  };

  const handleResendClick = async () => {
    if (resendRemainingSeconds > 0 || resending) return;
    setDigits(Array(OTP_LENGTH).fill(''));
    await onResend();
    setTimeout(() => inputRefs.current[0]?.focus(), 60);
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(43, 34, 27, 0.52)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1200,
        padding: '20px',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget && !loading) {
          onClose();
        }
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '420px',
          background: 'var(--color-surface)',
          borderRadius: '24px',
          padding: '36px 28px 28px',
          boxShadow: 'var(--shadow-lg)',
          border: '1px solid var(--color-border)',
          position: 'relative',
          textAlign: 'center',
          animation: 'scaleUp 0.2s ease-out',
        }}
      >
        <button
          type="button"
          onClick={onClose}
          disabled={loading}
          aria-label="Đóng cửa sổ nhập OTP"
          style={{
            position: 'absolute',
            top: '16px',
            right: '16px',
            width: '34px',
            height: '34px',
            borderRadius: '50%',
            background: 'var(--color-surface-muted)',
            border: '1px solid var(--color-border)',
            color: 'var(--color-text-secondary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.15rem',
            fontWeight: 700,
            lineHeight: 1,
          }}
        >
          ×
        </button>

        <h3
          style={{
            fontSize: '1.55rem',
            fontWeight: 800,
            color: 'var(--color-text-main)',
            marginBottom: '8px',
          }}
        >
          Nhập mã OTP
        </h3>

        <p
          style={{
            color: 'var(--color-text-secondary)',
            fontSize: '0.92rem',
            lineHeight: 1.5,
            marginBottom: '14px',
          }}
        >
          Chúng tôi đã gửi mã xác thực 6 số tới{' '}
          <span style={{ color: 'var(--color-primary-dark)', fontWeight: 700, wordBreak: 'break-word' }}>
            {email}
          </span>
        </p>

        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '6px 14px',
            borderRadius: 'var(--radius-pill)',
            fontSize: '0.84rem',
            fontWeight: 700,
            marginBottom: '18px',
            background: isExpired
              ? '#fee2e2'
              : isUrgent
                ? '#ffedd5'
                : 'var(--color-surface-muted)',
            color: isExpired
              ? '#b91c1c'
              : isUrgent
                ? '#c2410c'
                : 'var(--color-primary-dark)',
            border: `1px solid ${isExpired ? '#fecaca' : isUrgent ? '#fed7aa' : 'var(--color-border)'}`,
          }}
        >
          {isExpired ? (
            <span>Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.</span>
          ) : (
            <span>
              Mã có hiệu lực trong{' '}
              <strong style={{ fontVariantNumeric: 'tabular-nums' }}>
                {formatCountdown(otpRemainingSeconds)}
              </strong>
            </span>
          )}
        </div>

        {devOtpHint && (
          <div
            style={{
              padding: '10px 12px',
              background: '#fef7ed',
              color: 'var(--color-primary-dark)',
              border: '1px solid #f3d6b6',
              borderRadius: 'var(--radius-md)',
              marginBottom: '16px',
              fontSize: '0.84rem',
              fontWeight: 600,
            }}
          >
            {devOtpHint}
          </div>
        )}

        {errorMsg && (
          <div
            style={{
              padding: '10px 12px',
              background: '#fee2e2',
              color: '#b91c1c',
              borderRadius: 'var(--radius-md)',
              marginBottom: '16px',
              fontSize: '0.88rem',
              fontWeight: 600,
            }}
          >
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'center',
              gap: '10px',
              marginBottom: '24px',
            }}
            onPaste={handlePaste}
          >
            {digits.map((digit, index) => {
              const isFocused = focusedIndex === index;
              const hasValue = Boolean(digit);
              return (
                <input
                  key={index}
                  ref={(el) => {
                    inputRefs.current[index] = el;
                  }}
                  type="text"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={1}
                  value={digit}
                  disabled={loading}
                  aria-label={`Chữ số OTP thứ ${index + 1}`}
                  onFocus={() => setFocusedIndex(index)}
                  onChange={(e) => handleDigitChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  style={{
                    width: 'clamp(42px, 12vw, 50px)',
                    height: '54px',
                    borderRadius: '12px',
                    textAlign: 'center',
                    fontSize: '1.35rem',
                    fontWeight: 800,
                    fontVariantNumeric: 'tabular-nums',
                    color: 'var(--color-primary-dark)',
                    background: hasValue || isFocused ? 'var(--color-surface)' : 'var(--color-surface-muted)',
                    border: `1.5px solid ${
                      isFocused
                        ? 'var(--color-accent)'
                        : hasValue
                          ? 'var(--color-primary-light)'
                          : 'var(--color-border)'
                    }`,
                    boxShadow: isFocused ? '0 0 0 3px rgba(193, 120, 56, 0.18)' : 'none',
                    outline: 'none',
                    transition: 'var(--transition)',
                  }}
                />
              );
            })}
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={!canSubmit}
            style={{
              width: '100%',
              padding: '13px 20px',
              borderRadius: 'var(--radius-md)',
              fontSize: '1rem',
              fontWeight: 700,
              opacity: !canSubmit ? 0.55 : 1,
              cursor: !canSubmit ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? 'Đang xác thực...' : 'Xác thực OTP'}
          </button>
        </form>

        <div style={{ marginTop: '20px', fontSize: '0.88rem' }}>
          <div style={{ color: 'var(--color-text-secondary)', marginBottom: '4px' }}>
            Không nhận được mã?
          </div>
          {resendRemainingSeconds > 0 ? (
            <span
              style={{
                color: 'var(--color-text-muted)',
                fontWeight: 700,
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              Gửi lại mã OTP sau {formatCountdown(resendRemainingSeconds)}
            </span>
          ) : (
            <button
              type="button"
              onClick={handleResendClick}
              disabled={resending}
              style={{
                color: 'var(--color-accent)',
                fontWeight: 700,
                fontSize: '0.9rem',
                cursor: resending ? 'not-allowed' : 'pointer',
                opacity: resending ? 0.65 : 1,
              }}
            >
              {resending ? 'Đang gửi lại...' : 'Gửi lại mã OTP'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default OtpVerificationModal;
