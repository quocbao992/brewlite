import React, { useState, useEffect } from 'react';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { formatService } from '../../services/formatService';
import { resolveProductImage, defaultCoffeeImg } from '../../images';
import { toppingApi } from '../../api';

// Cache topping trong bộ nhớ để không phải gọi API lặp đi lặp lại
let cachedToppings = null;

export const ProductOrderModal = ({ product, isOpen, onClose, onNavigate }) => {
  const { isAuthenticated } = useAuth();
  const { addToCart } = useCart();

  const [selectedSize, setSelectedSize] = useState('M');
  const [selectedToppings, setSelectedToppings] = useState([]);
  const [quantity, setQuantity] = useState(1);
  const [note, setNote] = useState('');
  const [availableToppings, setAvailableToppings] = useState(cachedToppings || []);
  const [loadingToppings, setLoadingToppings] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (!isOpen || !product) return;
    setSelectedSize('M');
    setSelectedToppings([]);
    setQuantity(1);
    setNote('');
    setIsSuccess(false);

    // Chỉ gọi API toppings 1 lần duy nhất khi mở modal nếu chưa có cache
    if (!cachedToppings) {
      setLoadingToppings(true);
      toppingApi
        .getAll()
        .then((res) => {
          const list = Array.isArray(res) ? res : res?.data || [];
          cachedToppings = list;
          setAvailableToppings(list);
        })
        .catch(() => {
          setAvailableToppings([]);
        })
        .finally(() => {
          setLoadingToppings(false);
        });
    } else {
      setAvailableToppings(cachedToppings);
    }
  }, [isOpen, product]);

  if (!isOpen || !product) return null;

  const sizeExtra = selectedSize === 'L' ? 10000 : selectedSize === 'M' ? 5000 : 0;
  const toppingsExtra = selectedToppings.reduce(
    (sum, t) => sum + (Number(t.unitPrice) || 0),
    0
  );
  const unitCalculatedPrice = (product.unitPrice || 0) + sizeExtra + toppingsExtra;
  const totalPrice = unitCalculatedPrice * quantity;

  const handleToggleTopping = (topping) => {
    setSelectedToppings((prev) => {
      const exists = prev.some((t) => t.id === topping.id);
      if (exists) {
        return prev.filter((t) => t.id !== topping.id);
      }
      return [...prev, topping];
    });
  };

  const handleAddToCart = () => {
    if (!isAuthenticated) {
      onClose();
      if (onNavigate) onNavigate('login');
      return;
    }

    addToCart(product, quantity, selectedSize, note.trim(), selectedToppings);
    setIsSuccess(true);
    setTimeout(() => {
      setIsSuccess(false);
      onClose();
    }, 600);
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.55)',
        backdropFilter: 'blur(3px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '16px',
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--color-surface, #ffffff)',
          borderRadius: '16px',
          width: '100%',
          maxWidth: '460px',
          maxHeight: '90vh',
          overflowY: 'auto',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1)',
          border: '1px solid var(--color-border, #e2e8f0)',
          display: 'flex',
          flexDirection: 'column',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Hình ảnh món ở đầu tiên */}
        <div style={{ position: 'relative', width: '100%', height: '200px', overflow: 'hidden' }}>
          <img
            src={resolveProductImage(product)}
            alt={product.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            onError={(e) => {
              e.target.src = defaultCoffeeImg;
            }}
          />
          {product.categoryName && (
            <span
              style={{
                position: 'absolute',
                top: '12px',
                left: '12px',
                background: 'rgba(255, 255, 255, 0.95)',
                padding: '4px 10px',
                borderRadius: '999px',
                fontSize: '0.75rem',
                color: 'var(--color-primary-dark, #8b4513)',
                fontWeight: 600,
              }}
            >
              {product.categoryName}
            </span>
          )}
          <button
            onClick={onClose}
            style={{
              position: 'absolute',
              top: '12px',
              right: '12px',
              width: '32px',
              height: '32px',
              borderRadius: '50%',
              background: 'rgba(0, 0, 0, 0.5)',
              color: '#ffffff',
              border: 'none',
              fontSize: '1.1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            ✕
          </button>
        </div>

        {/* Nội dung bên dưới */}
        <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Tên món và giá cơ bản */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 600, margin: '0 0 4px', color: 'var(--color-text-main, #1e293b)' }}>
                {product.name}
              </h2>
              <span style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary, #64748b)' }}>
                Giá gốc: {formatService.formatMoney(product.unitPrice || 0)}
              </span>
            </div>
            <div style={{ fontSize: '1.3rem', fontWeight: 700, color: 'var(--color-accent, #c17838)' }}>
              {formatService.formatMoney(unitCalculatedPrice)}
            </div>
          </div>

          {/* Chọn Size */}
          <div>
            <div style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: '8px', color: 'var(--color-text-main, #1e293b)' }}>
              1. Chọn kích cỡ (Size)
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
              {[
                { size: 'S', label: 'Size S', extra: 0 },
                { size: 'M', label: 'Size M', extra: 5000 },
                { size: 'L', label: 'Size L', extra: 10000 },
              ].map((item) => {
                const isSelected = selectedSize === item.size;
                return (
                  <button
                    key={item.size}
                    type="button"
                    onClick={() => setSelectedSize(item.size)}
                    style={{
                      padding: '10px 8px',
                      borderRadius: '8px',
                      border: isSelected
                        ? '2px solid var(--color-primary-dark, #8b4513)'
                        : '1px solid var(--color-border, #e2e8f0)',
                      background: isSelected ? 'var(--color-primary-dark, #8b4513)' : '#f8fafc',
                      color: isSelected ? '#ffffff' : 'var(--color-text-main, #1e293b)',
                      fontWeight: isSelected ? 600 : 500,
                      cursor: 'pointer',
                      textAlign: 'center',
                      fontSize: '0.85rem',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '2px',
                    }}
                  >
                    <span>{item.label}</span>
                    <span style={{ fontSize: '0.75rem', opacity: isSelected ? 0.9 : 0.7 }}>
                      {item.extra > 0 ? `+${formatService.formatMoney(item.extra)}` : 'Tiêu chuẩn'}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Chọn Topping */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-main, #1e293b)' }}>
                2. Chọn Topping (tùy chọn)
              </span>
              {selectedToppings.length > 0 && (
                <button
                  type="button"
                  onClick={() => setSelectedToppings([])}
                  style={{
                    background: 'none',
                    border: 'none',
                    fontSize: '0.78rem',
                    color: '#ef4444',
                    cursor: 'pointer',
                    padding: 0,
                  }}
                >
                  Xóa chọn topping
                </button>
              )}
            </div>

            {loadingToppings ? (
              <div style={{ fontSize: '0.85rem', color: '#64748b', padding: '10px 0' }}>Đang tải danh sách topping...</div>
            ) : availableToppings.length === 0 ? (
              <div style={{ fontSize: '0.85rem', color: '#94a3b8', fontStyle: 'italic' }}>Không có topping khả dụng</div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '6px',
                  maxHeight: '140px',
                  overflowY: 'auto',
                  background: '#f8fafc',
                  padding: '10px',
                  borderRadius: '8px',
                  border: '1px solid var(--color-border, #e2e8f0)',
                }}
              >
                {availableToppings.map((tp) => {
                  const isChecked = selectedToppings.some((t) => t.id === tp.id);
                  return (
                    <label
                      key={tp.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '6px 8px',
                        borderRadius: '6px',
                        background: isChecked ? '#f1f5f9' : 'transparent',
                        cursor: 'pointer',
                        fontSize: '0.85rem',
                      }}
                    >
                      <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleToggleTopping(tp)}
                        />
                        <span>{tp.name}</span>
                      </span>
                      <span style={{ color: 'var(--color-accent, #c17838)', fontWeight: 600 }}>
                        +{formatService.formatMoney(tp.unitPrice || 0)}
                      </span>
                    </label>
                  );
                })}
              </div>
            )}
          </div>

          {/* Ghi chú */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px', color: 'var(--color-text-main, #1e293b)' }}>
              Ghi chú cho Barista (nếu có)
            </label>
            <input
              type="text"
              placeholder="VD: Ít ngọt, 50% đá, nhiều sữa..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
              style={{
                width: '100%',
                padding: '8px 12px',
                borderRadius: '8px',
                border: '1px solid var(--color-border, #e2e8f0)',
                fontSize: '0.85rem',
                boxSizing: 'border-box',
              }}
            />
          </div>

          {/* Số lượng & Nút Thêm vào giỏ */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '8px' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                border: '1px solid var(--color-border, #e2e8f0)',
                borderRadius: '8px',
                overflow: 'hidden',
              }}
            >
              <button
                type="button"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                style={{
                  width: '36px',
                  height: '38px',
                  background: '#f8fafc',
                  border: 'none',
                  fontSize: '1.1rem',
                  cursor: 'pointer',
                }}
              >
                -
              </button>
              <span style={{ width: '36px', textAlign: 'center', fontWeight: 600, fontSize: '0.9rem' }}>
                {quantity}
              </span>
              <button
                type="button"
                onClick={() => setQuantity((q) => q + 1)}
                style={{
                  width: '36px',
                  height: '38px',
                  background: '#f8fafc',
                  border: 'none',
                  fontSize: '1.1rem',
                  cursor: 'pointer',
                }}
              >
                +
              </button>
            </div>

            <button
              type="button"
              className="btn btn-primary"
              onClick={handleAddToCart}
              style={{
                flex: 1,
                padding: '10px 16px',
                borderRadius: '8px',
                fontSize: '0.95rem',
                fontWeight: 600,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <span>{isSuccess ? '✓ Đã thêm vào giỏ!' : 'Thêm vào giỏ hàng'}</span>
              <span>{formatService.formatMoney(totalPrice)}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductOrderModal;

