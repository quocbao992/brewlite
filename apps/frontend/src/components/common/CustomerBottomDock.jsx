import React from 'react';
import {
  IconHome,
  IconGrid,
  IconOrders,
  IconCart,
  IconUser,
  IconShield,
} from './Icons';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';

export const CustomerBottomDock = ({ currentTab, onNavigate }) => {
  const { isAuthenticated, isAdmin, isBarista, user } = useAuth();
  const { totalItems } = useCart();

  return (
    <div className="customer-bottom-dock" role="navigation" aria-label="Thanh điều hướng người dùng">
      {/* 1. Trang chủ */}
      <button
        type="button"
        className={`tool-square-btn ${currentTab === 'home' ? 'active' : ''}`}
        onClick={() => onNavigate('home')}
        aria-label="Trang chủ"
        title="Trang chủ"
      >
        <IconHome size={22} />
        <span className="dock-tooltip dock-tooltip-top">Trang chủ</span>
      </button>

      {/* 2. Thực đơn */}
      <button
        type="button"
        className={`tool-square-btn ${currentTab === 'menu' ? 'active' : ''}`}
        onClick={() => onNavigate('menu')}
        aria-label="Thực đơn"
        title="Thực đơn"
      >
        <IconGrid size={22} />
        <span className="dock-tooltip dock-tooltip-top">Thực đơn</span>
      </button>

      {/* 3. Đơn hàng của tôi */}
      <button
        type="button"
        className={`tool-square-btn ${currentTab === 'orders' ? 'active' : ''}`}
        onClick={() => onNavigate(isAuthenticated ? 'orders' : 'login')}
        aria-label="Đơn hàng của tôi"
        title={isAuthenticated ? 'Đơn hàng của tôi' : 'Đăng nhập xem đơn'}
      >
        <IconOrders size={22} />
        <span className="dock-tooltip dock-tooltip-top">
          {isAuthenticated ? 'Đơn hàng của tôi' : 'Đăng nhập xem đơn'}
        </span>
      </button>

      {/* 4. Giỏ hàng */}
      <button
        type="button"
        className={`tool-square-btn ${currentTab === 'cart' ? 'active' : ''}`}
        onClick={() => onNavigate('cart')}
        aria-label="Giỏ hàng"
        title={`Giỏ hàng ${totalItems > 0 ? `(${totalItems})` : ''}`}
      >
        <IconCart size={22} />
        {totalItems > 0 && <span className="tool-badge">{totalItems > 99 ? '99+' : totalItems}</span>}
        <span className="dock-tooltip dock-tooltip-top">
          Giỏ hàng {totalItems > 0 ? `(${totalItems})` : ''}
        </span>
      </button>

      {/* 5. Hồ sơ cá nhân */}
      <button
        type="button"
        className={`tool-square-btn ${currentTab === 'profile' ? 'active' : ''}`}
        onClick={() => onNavigate(isAuthenticated ? 'profile' : 'login')}
        aria-label="Hồ sơ cá nhân"
        title={isAuthenticated ? (user?.fullName || user?.username || 'Hồ sơ cá nhân') : 'Đăng nhập'}
      >
        <IconUser size={22} />
        <span className="dock-tooltip dock-tooltip-top">
          {isAuthenticated ? (user?.fullName || user?.username || 'Hồ sơ cá nhân') : 'Đăng nhập'}
        </span>
      </button>

      {/* 6. Nút Admin / Barista chuyển nhanh */}
      {isAdmin && (
        <button
          type="button"
          className={`tool-square-btn ${currentTab === 'admin' ? 'active' : ''}`}
          onClick={() => onNavigate('admin')}
          aria-label="Quản trị Admin"
          title="Quản trị Admin"
        >
          <IconShield size={22} />
          <span className="dock-tooltip dock-tooltip-top">Quản trị Admin</span>
        </button>
      )}

      {isBarista && !isAdmin && (
        <button
          type="button"
          className={`tool-square-btn ${currentTab === 'admin' ? 'active' : ''}`}
          onClick={() => onNavigate('admin')}
          aria-label="Quầy Barista"
          title="Quầy Barista (Xác nhận đơn)"
        >
          <IconShield size={22} />
          <span className="dock-tooltip dock-tooltip-top">Quầy Barista (Xác nhận đơn)</span>
        </button>
      )}
    </div>
  );
};

export default CustomerBottomDock;
