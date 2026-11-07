import React from 'react';

export const LoadingSpinner = ({ text = 'Đang tải dữ liệu...' }) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '60px 0',
        gap: '16px',
      }}
    >
      <div
        style={{
          width: '42px',
          height: '42px',
          border: '4px solid #f3efe9',
          borderTop: '4px solid var(--color-accent)',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }}
      />
      <span style={{ color: 'var(--color-text-secondary)', fontWeight: 600 }}>
        {text}
      </span>
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};

export default LoadingSpinner;
