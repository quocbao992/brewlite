import React from 'react';

export const Footer = () => {
  return (
    <footer
      style={{
        background: 'var(--color-primary-dark, #3b220e)',
        borderTop: '1px solid rgba(255, 255, 255, 0.08)',
        padding: '20px 24px',
        textAlign: 'center',
        color: '#d5ccc3',
        fontSize: '0.88rem',
      }}
    >
      <div className="container">
        © {new Date().getFullYear()} BrewLite Coffee. Nền tảng đặt đồ uống trực tuyến.
      </div>
    </footer>
  );
};

export default Footer;
