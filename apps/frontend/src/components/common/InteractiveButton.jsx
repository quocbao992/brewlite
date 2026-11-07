import React from 'react';

export const InteractiveButton = ({
  text = 'Xác nhận',
  loadingText = 'Đang xử lý...',
  successText = 'Thành công!',
  loading = false,
  success = false,
  onClick,
  type = 'submit',
  disabled = false,
  style = {},
  className = '',
}) => {
  return (
    <button
      type={type}
      disabled={disabled || loading || success}
      onClick={onClick}
      className={`interactive-btn ${loading ? 'is-loading' : ''} ${success ? 'is-success' : ''} ${className}`}
      style={{
        position: 'relative',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '10px',
        width: '100%',
        padding: '14px 24px',
        fontSize: '1rem',
        fontWeight: 700,
        color: '#ffffff',
        background: success
          ? 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)'
          : 'linear-gradient(135deg, var(--color-primary, #8a5327) 0%, var(--color-primary-dark, #633614) 100%)',
        border: 'none',
        borderRadius: 'var(--radius-md, 10px)',
        cursor: disabled || loading || success ? 'not-allowed' : 'pointer',
        boxShadow: success
          ? '0 4px 14px rgba(22, 163, 74, 0.35)'
          : '0 4px 14px rgba(138, 83, 39, 0.25)',
        transition: 'all 0.35s cubic-bezier(0.4, 0, 0.2, 1)',
        overflow: 'hidden',
        outline: 'none',
        ...style,
      }}
    >
      {/* Hiệu ứng shimmer ánh sáng chạy qua khi hover */}
      <span className="interactive-btn-shimmer" />

      {loading ? (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            style={{ animation: 'spin 0.8s linear infinite' }}
          >
            <circle cx="12" cy="12" r="10" strokeOpacity="0.25" />
            <path d="M12 2a10 10 0 0 1 10 10" />
          </svg>
          <span>{loadingText}</span>
        </span>
      ) : success ? (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', animation: 'scaleUp 0.3s ease-out' }}>
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#ffffff"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
          <span>{successText}</span>
        </span>
      ) : (
        <span>{text}</span>
      )}
    </button>
  );
};

export default InteractiveButton;
