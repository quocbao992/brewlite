import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { IconShield } from './Icons';

export const Navbar = ({ currentTab, onNavigate }) => {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { totalItems } = useCart();

  return (
    <nav className="navbar">
      <div className="container nav-inner">
        <div
          className="logo"
          style={{ cursor: 'pointer' }}
          onClick={() => onNavigate('home')}
        >
          BREWLITE
          <span className="logo-badge">Coffee</span>
        </div>

        <div className="nav-actions">
          {isAdmin && (
            <button
              className="btn btn-sm"
              style={{
                background: currentTab === 'admin' ? '#16a34a' : 'var(--color-surface, #ffffff)',
                color: currentTab === 'admin' ? '#ffffff' : 'var(--color-text-main, #374151)',
                fontWeight: 700,
                borderRadius: '10px',
                padding: '7px 14px',
                border: '1px solid var(--color-border, #e2e8f0)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
              }}
              onClick={() => onNavigate('admin')}
              title="Chuyển sang Bảng Quản Trị Hệ Thống"
            >
              <IconShield size={18} color={currentTab === 'admin' ? '#ffffff' : 'var(--color-text-main, #374151)'} />
              <span>Quản trị Admin</span>
            </button>
          )}

          <button
            className="cart-btn"
            title="Xem giỏ hàng"
            onClick={() => onNavigate('cart')}
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="9" cy="21" r="1" />
              <circle cx="20" cy="21" r="1" />
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
            </svg>
            {totalItems > 0 && <span className="cart-count">{totalItems}</span>}
          </button>

          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                className={`btn btn-sm ${currentTab === 'profile' ? 'btn-primary' : 'btn-secondary'}`}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontWeight: 600,
                  padding: '5px 14px 5px 6px',
                  borderRadius: '30px',
                }}
                onClick={() => onNavigate('profile')}
                title="Xem và chỉnh sửa thông tin cá nhân"
              >
                <span
                  style={{
                    width: '26px',
                    height: '26px',
                    borderRadius: '50%',
                    background: currentTab === 'profile' ? '#ffffff' : 'var(--color-primary, #8a5327)',
                    color: currentTab === 'profile' ? 'var(--color-primary, #8a5327)' : '#ffffff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.85rem',
                    fontWeight: 800,
                    textTransform: 'uppercase',
                  }}
                >
                  {(user?.fullName || user?.username || 'U').trim().charAt(0)}
                </span>
                <span>{user?.fullName || user?.username}</span>
              </button>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  logout();
                  onNavigate('home');
                }}
              >
                Đăng xuất
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => onNavigate('login')}
              >
                Đăng nhập
              </button>
              <button
                className="btn btn-primary btn-sm"
                onClick={() => onNavigate('register')}
              >
                Đăng ký
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
