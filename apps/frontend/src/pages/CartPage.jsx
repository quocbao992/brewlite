import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { formatService } from '../services/formatService';
import { resolveProductImage, defaultCoffeeImg } from '../images';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EditCartItemModal from '../components/common/EditCartItemModal';

export const CartPage = ({ onNavigate }) => {
  const { isAuthenticated } = useAuth();
  const {
    cartItems,
    subtotal,
    totalItems,
    updateQuantity,
    updateItem,
    removeFromCart,
    clearCart,
  } = useCart();

  const [loadingCart, setLoadingCart] = useState(true);
  const [editingItem, setEditingItem] = useState(null);

  useEffect(() => {
    if (isAuthenticated) {
      setLoadingCart(true);
      const timer = setTimeout(() => {
        setLoadingCart(false);
      }, 500);
      return () => clearTimeout(timer);
    } else {
      setLoadingCart(false);
    }
  }, [isAuthenticated]);

  if (!isAuthenticated) {
    return (
      <div className="container" style={{ padding: '80px 24px', textAlign: 'center' }}>
        <h2 style={{ fontSize: '1.8rem', fontWeight: 800, marginBottom: '8px' }}>
          Vui Lòng Đăng Nhập Để Xem Giỏ Hàng
        </h2>
        <p
          style={{
            color: 'var(--color-text-secondary)',
            marginBottom: '24px',
            maxWidth: '460px',
            margin: '0 auto 24px',
          }}
        >
          Bạn cần đăng nhập tài khoản thành viên để thêm món, chỉnh sửa giỏ hàng và tiến hành đặt hàng.
        </p>
        <button className="btn btn-primary" onClick={() => onNavigate('login')}>
          Đăng nhập ngay để đặt hàng
        </button>
      </div>
    );
  }

  if (loadingCart) {
    return (
      <div className="container" style={{ padding: '90px 24px', textAlign: 'center' }}>
        <LoadingSpinner text="Đang tải giỏ hàng của bạn..." />
      </div>
    );
  }

  if (cartItems.length === 0) {
    return (
      <div className="container" style={{ padding: '80px 24px', textAlign: 'center' }}>
        <h2 style={{ fontSize: '1.8rem', fontWeight: 800, marginBottom: '8px' }}>
          Giỏ hàng của bạn đang trống
        </h2>
        <p style={{ color: 'var(--color-text-secondary)', marginBottom: '24px' }}>
          Hãy chọn cho mình một ly đồ uống thơm ngon để nạp năng lượng nhé!
        </p>
        <button className="btn btn-primary" onClick={() => onNavigate('menu')}>
          Khám phá thực đơn ngay
        </button>
      </div>
    );
  }

  const handleStartEditNote = (item) => {
    setEditingNoteId(item.id);
    setTempNote(item.note || '');
  };

  const handleSaveNote = (cartItemId) => {
    updateItem(cartItemId, { note: tempNote.trim() });
    setEditingNoteId(null);
  };

  const handleCheckoutClick = () => {
    if (!isAuthenticated) {
      onNavigate('login');
      return;
    }
    onNavigate('checkout');
  };

  return (
    <div className="container" style={{ padding: '40px 24px 60px' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '28px',
        }}
      >
        <h1 style={{ fontSize: '2.2rem', fontWeight: 800 }}>
          Giỏ Hàng Của Bạn ({totalItems} món)
        </h1>
        <button
          className="btn btn-outline btn-sm"
          onClick={clearCart}
          style={{ borderColor: 'var(--color-danger)', color: 'var(--color-danger)' }}
        >
          Xóa toàn bộ
        </button>
      </div>

      <div className="cart-page-grid">
        <div>
          <table className="cart-table">
            <thead>
              <tr>
                <th>Món đồ uống</th>
                <th>Kích cỡ</th>
                <th>Đơn giá</th>
                <th>Số lượng</th>
                <th>Thành tiền</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {cartItems.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                      <img
                        src={item.img || resolveProductImage(item)}
                        alt={item.name}
                        style={{
                          width: '54px',
                          height: '54px',
                          borderRadius: '10px',
                          objectFit: 'cover',
                          border: '1px solid var(--color-border)',
                        }}
                        onError={(e) => {
                          e.target.src = defaultCoffeeImg;
                        }}
                      />
                      <div>
                        <div style={{ fontSize: '0.95rem' }}>{item.name}</div>
                        {item.toppings && item.toppings.length > 0 && (
                          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
                            Topping: {item.toppings.map((t) => t.name).join(', ')}
                          </div>
                        )}
                          <div
                            style={{
                              fontSize: '0.82rem',
                              color: 'var(--color-text-muted)',
                              marginTop: '2px',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '8px',
                            }}
                          >
                            <span>{item.note ? `Ghi chú: ${item.note}` : 'Chưa có ghi chú'}</span>
                            <button
                              type="button"
                              onClick={() => setEditingItem(item)}
                              style={{
                                color: 'var(--color-accent)',
                                textDecoration: 'underline',
                                fontSize: '0.82rem',
                                cursor: 'pointer',
                                fontWeight: 600,
                                background: 'transparent',
                                border: 'none',
                                padding: 0,
                              }}
                              title="Chỉnh sửa size, topping, số lượng và ghi chú"
                            >
                              Sửa
                            </button>
                          </div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '4px' }}>
                      {['S', 'M', 'L'].map((sz) => (
                        <button
                          key={sz}
                          onClick={() => updateItem(item.id, { size: sz })}
                          style={{
                            padding: '4px 8px',
                            borderRadius: '6px',
                            fontSize: '0.76rem',
                            fontWeight: 700,
                            border: '1px solid var(--color-border)',
                            background:
                              item.size === sz
                                ? 'var(--color-primary-dark)'
                                : 'var(--color-surface-muted)',
                            color: item.size === sz ? '#fff' : 'var(--color-text-secondary)',
                            cursor: 'pointer',
                            transition: 'var(--transition)',
                          }}
                          title={`Chuyển sang Size ${sz}`}
                        >
                          {sz}
                        </button>
                      ))}
                    </div>
                  </td>
                  <td>{formatService.formatMoney(item.unitPrice)}</td>
                  <td>
                    <div className="qty-control">
                      <button
                        className="qty-btn"
                        onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      >
                        -
                      </button>
                      <span className="qty-number">{item.quantity}</span>
                      <button
                        className="qty-btn"
                        onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      >
                        +
                      </button>
                    </div>
                  </td>
                  <td style={{ fontWeight: 700, color: 'var(--color-accent)' }}>
                    {formatService.formatMoney(
                      item.itemTotalPrice || item.unitPrice * item.quantity
                    )}
                  </td>
                  <td>
                    <button
                      onClick={() => removeFromCart(item.id)}
                      style={{
                        color: 'var(--color-danger)',
                        fontSize: '0.85rem',
                        fontWeight: 600,
                        cursor: 'pointer',
                        padding: '4px 8px',
                        background: 'none',
                        border: '1px solid var(--color-danger)',
                        borderRadius: '4px',
                      }}
                      title="Xóa món"
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div>
          <div className="order-summary-card">
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '20px' }}>
              Tóm tắt đơn hàng
            </h3>

            <div className="summary-row">
              <span style={{ color: 'var(--color-text-secondary)' }}>Tạm tính</span>
              <span style={{ fontWeight: 700 }}>{formatService.formatMoney(subtotal)}</span>
            </div>

            <div className="summary-row">
              <span style={{ color: 'var(--color-text-secondary)' }}>Giảm giá</span>
              <span style={{ color: 'var(--color-success)', fontWeight: 700 }}>0 ₫</span>
            </div>

            <div className="summary-row total">
              <span>Tổng thanh toán</span>
              <span>{formatService.formatMoney(subtotal)}</span>
            </div>

            <button
              className="btn btn-primary"
              style={{ width: '100%', marginTop: '24px', padding: '14px' }}
              onClick={handleCheckoutClick}
            >
              Tiến hành thanh toán
            </button>
          </div>
        </div>
      </div>

      {/* MODAL POPUP CHỈNH SỬA MÓN (Size, Topping, Số lượng, Ghi chú) */}
      <EditCartItemModal
        item={editingItem}
        isOpen={Boolean(editingItem)}
        onClose={() => setEditingItem(null)}
        onSave={updateItem}
      />
    </div>
  );
};

export default CartPage;
