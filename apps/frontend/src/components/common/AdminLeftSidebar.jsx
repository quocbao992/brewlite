import React from 'react';
import {
  IconDashboard,
  IconOrders,
  IconCoffee,
  IconCategory,
  IconTopping,
  IconWarehouse,
  IconPromotion,
  IconUsers,
  IconWeather,
  IconStore,
  IconLogout,
} from './Icons';
import { useAuth } from '../../context/AuthContext';

export const AdminLeftSidebar = ({ adminTab, onSelectTab, onNavigateToCustomer }) => {
  const { logout, user, isBarista } = useAuth();

  return (
    <aside className="admin-left-sidebar" aria-label="Thanh công cụ quản trị hệ thống">
      <div
        className="admin-sidebar-logo"
        onClick={() => onSelectTab(isBarista ? 'orders' : 'dashboard')}
        title={isBarista ? 'BrewLite Barista Station' : 'BrewLite Admin'}
      >
        {isBarista ? 'BA' : 'BL'}
      </div>

      <nav className="admin-sidebar-nav">
        {/* Nếu là Barista: CHỈ hiển thị duy nhất Quản lý Đơn hàng */}
        {isBarista ? (
          <button
            type="button"
            className={`tool-square-btn ${adminTab === 'orders' ? 'active' : ''}`}
            onClick={() => onSelectTab('orders')}
            aria-label="Hàng đợi Barista & Xác nhận đơn"
            title="Hàng đợi Barista & Xác nhận đơn"
          >
            <IconOrders size={22} />
            <span className="dock-tooltip dock-tooltip-right">Quầy Pha Chế / Xác Nhận Đơn</span>
          </button>
        ) : (
          <>
            {/* 1. Tổng quan */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'dashboard' ? 'active' : ''}`}
              onClick={() => onSelectTab('dashboard')}
              aria-label="Tổng quan hệ thống"
              title="Tổng quan (Dashboard)"
            >
              <IconDashboard size={22} />
              <span className="dock-tooltip dock-tooltip-right">Tổng quan (Dashboard)</span>
            </button>

            {/* 2. Đơn hàng */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'orders' ? 'active' : ''}`}
              onClick={() => onSelectTab('orders')}
              aria-label="Quản lý đơn hàng"
              title="Quản lý Đơn hàng"
            >
              <IconOrders size={22} />
              <span className="dock-tooltip dock-tooltip-right">Quản lý Đơn hàng</span>
            </button>

            {/* 3. Sản phẩm */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'products' ? 'active' : ''}`}
              onClick={() => onSelectTab('products')}
              aria-label="Quản lý sản phẩm"
              title="Quản lý Sản phẩm"
            >
              <IconCoffee size={22} />
              <span className="dock-tooltip dock-tooltip-right">Quản lý Sản phẩm</span>
            </button>

            {/* 4. Topping */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'toppings' ? 'active' : ''}`}
              onClick={() => onSelectTab('toppings')}
              aria-label="Quản lý topping"
              title="Quản lý Topping"
            >
              <IconTopping size={22} />
              <span className="dock-tooltip dock-tooltip-right">Quản lý Topping</span>
            </button>

            {/* 5. Quản lý Kho */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'inventory' ? 'active' : ''}`}
              onClick={() => onSelectTab('inventory')}
              aria-label="Quản lý kho hàng"
              title="Quản lý Kho hàng"
            >
              <IconWarehouse size={22} />
              <span className="dock-tooltip dock-tooltip-right">Quản lý Kho hàng</span>
            </button>

            {/* 6. Quản lý Khuyến mãi */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'promotions' ? 'active' : ''}`}
              onClick={() => onSelectTab('promotions')}
              aria-label="Quản lý khuyến mãi"
              title="Quản lý Khuyến mãi & Voucher"
            >
              <IconPromotion size={22} />
              <span className="dock-tooltip dock-tooltip-right">Quản lý Khuyến mãi & Voucher</span>
            </button>

            {/* 7. Danh mục */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'categories' ? 'active' : ''}`}
              onClick={() => onSelectTab('categories')}
              aria-label="Quản lý danh mục"
              title="Quản lý Danh mục"
            >
              <IconCategory size={22} />
              <span className="dock-tooltip dock-tooltip-right">Quản lý Danh mục</span>
            </button>

            {/* 7. Người dùng */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'users' ? 'active' : ''}`}
              onClick={() => onSelectTab('users')}
              aria-label="Quản lý người dùng"
              title="Quản lý Người dùng"
            >
              <IconUsers size={22} />
              <span className="dock-tooltip dock-tooltip-right">Quản lý Người dùng</span>
            </button>

            {/* 8. Dự báo Thời tiết */}
            <button
              type="button"
              className={`tool-square-btn ${adminTab === 'weather' ? 'active' : ''}`}
              onClick={() => onSelectTab('weather')}
              aria-label="Thời tiết & Gợi ý"
              title="Dự báo thời tiết"
            >
              <IconWeather size={22} />
              <span className="dock-tooltip dock-tooltip-right">Dự báo thời tiết</span>
            </button>
          </>
        )}
      </nav>

      <div className="admin-sidebar-bottom">
        <button
          type="button"
          className="tool-square-btn"
          onClick={onNavigateToCustomer}
          aria-label="Xem trang khách hàng"
          title="Xem trang Khách hàng"
        >
          <IconStore size={22} />
          <span className="dock-tooltip dock-tooltip-right">Xem trang Khách hàng</span>
        </button>

        <button
          type="button"
          className="tool-square-btn"
          onClick={() => {
            logout();
            onNavigateToCustomer();
          }}
          aria-label="Đăng xuất"
          title={`Đăng xuất (${user?.username || (isBarista ? 'Barista' : 'Admin')})`}
          style={{ color: '#ef4444' }}
        >
          <IconLogout size={22} />
          <span className="dock-tooltip dock-tooltip-right">
            Đăng xuất ({user?.username || (isBarista ? 'Barista' : 'Admin')})
          </span>
        </button>
      </div>
    </aside>
  );
};

export default AdminLeftSidebar;
