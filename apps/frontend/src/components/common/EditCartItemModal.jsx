import React, { useState, useEffect } from 'react';
import { formatService } from '../../services/formatService';
import { resolveProductImage, defaultCoffeeImg } from '../../images';
import { toppingApi } from '../../api';

let cachedToppings = null;

export const EditCartItemModal = ({ item, isOpen, onClose, onSave }) => {
  const [selectedSize, setSelectedSize] = useState('M');
  const [selectedToppings, setSelectedToppings] = useState([]);
  const [quantity, setQuantity] = useState(1);
  const [note, setNote] = useState('');
  const [availableToppings, setAvailableToppings] = useState(cachedToppings || []);
  const [loadingToppings, setLoadingToppings] = useState(false);

  useEffect(() => {
    if (!isOpen || !item) return;

    setSelectedSize(item.size || 'M');
    setSelectedToppings(item.toppings ? [...item.toppings] : []);
    setQuantity(item.quantity || 1);
    setNote(item.note || '');

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
  }, [isOpen, item]);

  if (!isOpen || !item) return null;

  // Tính lại đơn giá gốc (base) của món
  const currentSizeExtra = item.size === 'L' ? 10000 : item.size === 'M' ? 5000 : 0;
  const currentToppingsExtra = (item.toppings || []).reduce(
    (sum, t) => sum + (Number(t.price || t.unitPrice) || 0),
    0
  );
  const baseUnitPrice =
    item.baseUnitPrice !== undefined
      ? item.baseUnitPrice
      : Math.max(0, (item.unitPrice || 0) - currentSizeExtra - currentToppingsExtra);

  const newSizeExtra = selectedSize === 'L' ? 10000 : selectedSize === 'M' ? 5000 : 0;
  const newToppingsExtra = selectedToppings.reduce(
    (sum, t) => sum + (Number(t.price || t.unitPrice) || 0),
    0
  );
  const calculatedUnitPrice = baseUnitPrice + newSizeExtra + newToppingsExtra;
  const calculatedTotal = calculatedUnitPrice * quantity;

  const handleToggleTopping = (topping) => {
    setSelectedToppings((prev) => {
      const exists = prev.some((t) => (t.id || t.toppingId) === (topping.id || topping.toppingId));
      if (exists) {
        return prev.filter((t) => (t.id || t.toppingId) !== (topping.id || topping.toppingId));
      }
      return [
        ...prev,
        {
          id: topping.id,
          toppingId: topping.id,
          name: topping.name,
          price: topping.price || topping.unitPrice || 0,
          unitPrice: topping.price || topping.unitPrice || 0,
        },
      ];
    });
  };

  const handleSave = () => {
    if (onSave) {
      onSave(item.id, {
        size: selectedSize,
        toppings: selectedToppings,
        quantity,
        note: note.trim(),
        baseUnitPrice,
      });
    }
    onClose();
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
        zIndex: 1100,
        padding: '16px',
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: '#ffffff',
          borderRadius: '16px',
          width: '100%',
          maxWidth: '460px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
          overflow: 'hidden',
          animation: 'fadeInScale 0.2s ease-out forwards',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* HEADER: Ảnh + Tên món */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
            padding: '16px 20px',
            borderBottom: '1px solid #e2e8f0',
            background: '#fafafa',
          }}
        >
          <img
            src={item.img || resolveProductImage(item)}
            alt={item.name}
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '10px',
              objectFit: 'cover',
              border: '1px solid #e2e8f0',
              flexShrink: 0,
            }}
            onError={(e) => {
              e.target.src = defaultCoffeeImg;
            }}
          />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: '0.78rem', color: '#64748b', fontWeight: 600 }}>CHỈNH SỬA MÓN</div>
            <h3
              style={{
                fontSize: '1.1rem',
                fontWeight: 700,
                color: '#1e293b',
                margin: '2px 0 0',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {item.name}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              fontSize: '1.3rem',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '4px',
              lineHeight: 1,
            }}
          >
            ✕
          </button>
        </div>

        {/* NỘI DUNG CHỈNH SỬA (SCROLLABLE) */}
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1 }}>
          {/* 1. Chọn Size */}
          <div style={{ marginBottom: '18px' }}>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 700, marginBottom: '8px', color: '#334155' }}>
              Kích cỡ (Size):
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
              {[
                { size: 'S', label: 'Size S', extra: 0 },
                { size: 'M', label: 'Size M', extra: 5000 },
                { size: 'L', label: 'Size L', extra: 10000 },
              ].map((s) => {
                const isSelected = selectedSize === s.size;
                return (
                  <button
                    key={s.size}
                    type="button"
                    onClick={() => setSelectedSize(s.size)}
                    style={{
                      padding: '10px 4px',
                      borderRadius: '8px',
                      border: isSelected ? '2px solid var(--color-accent, #c17838)' : '1px solid #cbd5e1',
                      background: isSelected ? 'rgba(193, 120, 56, 0.08)' : '#ffffff',
                      color: isSelected ? 'var(--color-accent, #c17838)' : '#475569',
                      fontWeight: isSelected ? 700 : 500,
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '2px',
                    }}
                  >
                    <span>{s.label}</span>
                    <span style={{ fontSize: '0.72rem', opacity: 0.85 }}>
                      {s.extra === 0 ? 'Tiêu chuẩn' : `+${formatService.formatMoney(s.extra)}`}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* 2. Chọn Topping */}
          <div style={{ marginBottom: '18px' }}>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 700, marginBottom: '8px', color: '#334155' }}>
              Món thêm (Topping):
            </label>
            {loadingToppings ? (
              <div style={{ fontSize: '0.85rem', color: '#64748b', fontStyle: 'italic' }}>Đang nạp topping...</div>
            ) : availableToppings.length === 0 ? (
              <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Hiện không có topping khả dụng.</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '160px', overflowY: 'auto' }}>
                {availableToppings.map((tp) => {
                  const isChecked = selectedToppings.some((t) => (t.id || t.toppingId) === tp.id);
                  const price = Number(tp.price || tp.unitPrice) || 0;
                  return (
                    <label
                      key={tp.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '8px 12px',
                        borderRadius: '8px',
                        border: isChecked ? '1px solid var(--color-accent, #c17838)' : '1px solid #e2e8f0',
                        background: isChecked ? '#fffbeb' : '#ffffff',
                        cursor: 'pointer',
                        fontSize: '0.85rem',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleToggleTopping(tp)}
                          style={{ cursor: 'pointer', accentColor: 'var(--color-accent, #c17838)' }}
                        />
                        <span style={{ fontWeight: isChecked ? 600 : 400 }}>{tp.name}</span>
                      </div>
                      <span style={{ fontWeight: 600, color: 'var(--color-accent, #c17838)', fontSize: '0.82rem' }}>
                        +{formatService.formatMoney(price)}
                      </span>
                    </label>
                  );
                })}
              </div>
            )}
          </div>

          {/* 3. Số lượng */}
          <div style={{ marginBottom: '18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <label style={{ fontSize: '0.88rem', fontWeight: 700, color: '#334155' }}>Số lượng:</label>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', padding: '2px 8px' }}>
              <button
                type="button"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                style={{ background: 'transparent', border: 'none', fontSize: '1.2rem', cursor: 'pointer', padding: '0 6px', color: '#475569' }}
              >
                -
              </button>
              <span style={{ fontWeight: 700, fontSize: '0.95rem', minWidth: '24px', textAlign: 'center' }}>
                {quantity}
              </span>
              <button
                type="button"
                onClick={() => setQuantity((q) => q + 1)}
                style={{ background: 'transparent', border: 'none', fontSize: '1.2rem', cursor: 'pointer', padding: '0 6px', color: '#475569' }}
              >
                +
              </button>
            </div>
          </div>

          {/* 4. Ghi chú */}
          <div>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 700, marginBottom: '6px', color: '#334155' }}>
              Ghi chú riêng:
            </label>
            <input
              type="text"
              className="form-input"
              placeholder="VD: Ít đường, nhiều đá, để riêng..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
              style={{ width: '100%', fontSize: '0.85rem' }}
            />
          </div>
        </div>

        {/* FOOTER: Giá + Nút Lưu */}
        <div
          style={{
            padding: '16px 20px',
            borderTop: '1px solid #e2e8f0',
            background: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
          }}
        >
          <div>
            <div style={{ fontSize: '0.75rem', color: '#64748b' }}>Tổng món</div>
            <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--color-accent, #c17838)' }}>
              {formatService.formatMoney(calculatedTotal)}
            </div>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onClose}
              style={{ padding: '10px 16px', borderRadius: '8px' }}
            >
              Hủy
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handleSave}
              style={{ padding: '10px 20px', borderRadius: '8px', fontWeight: 700 }}
            >
              Cập nhật món
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditCartItemModal;

