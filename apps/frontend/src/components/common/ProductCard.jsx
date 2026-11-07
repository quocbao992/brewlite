import React, { useState } from 'react';
import { formatService } from '../../services/formatService';
import { resolveProductImage, defaultCoffeeImg } from '../../images';
import ProductOrderModal from './ProductOrderModal';

export const ProductCard = ({ product, onNavigate }) => {
  const [showModal, setShowModal] = useState(false);

  return (
    <>
      <div
        className="product-card"
        style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          height: '100%',
          padding: '16px',
          boxSizing: 'border-box',
          borderRadius: '12px',
          border: '1px solid var(--color-border, #e2e8f0)',
          background: 'var(--color-surface, #ffffff)',
          transition: 'all 0.2s ease',
        }}
      >
        <div>
          {/* Ảnh sản phẩm */}
          <div
            className="card-img-wrap"
            style={{
              width: '100%',
              height: '180px',
              borderRadius: '8px',
              overflow: 'hidden',
              position: 'relative',
              background: '#f8fafc',
              marginBottom: '12px',
            }}
          >
            <img
              src={resolveProductImage(product)}
              alt={product.name}
              className="card-img"
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              onError={(e) => {
                e.target.src = defaultCoffeeImg;
              }}
            />
            {product.categoryName && (
              <span
                className="card-badge"
                style={{
                  position: 'absolute',
                  top: '8px',
                  left: '8px',
                  background: 'rgba(255, 255, 255, 0.92)',
                  backdropFilter: 'blur(4px)',
                  padding: '3px 8px',
                  borderRadius: '999px',
                  fontSize: '0.75rem',
                  color: 'var(--color-primary-dark, #8b4513)',
                  fontWeight: 500,
                }}
              >
                {product.categoryName}
              </span>
            )}
          </div>

          {/* Tên món - không tự ý in đậm quá mức, dùng chữ vừa vặn thanh lịch */}
          <h3
            className="card-title"
            style={{
              fontSize: '1rem',
              fontWeight: 500,
              color: 'var(--color-text-main, #1e293b)',
              marginBottom: '8px',
              lineHeight: 1.4,
              minHeight: '2.8em',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden',
            }}
          >
            {product.name}
          </h3>
        </div>

        {/* Chân card: Giá tiền và Nút "Đặt món" */}
        <div
          className="card-meta"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingTop: '12px',
            borderTop: '1px solid var(--color-surface-muted, #f1f5f9)',
            marginTop: 'auto',
          }}
        >
          <div
            className="card-price"
            style={{
              fontSize: '1.05rem',
              fontWeight: 600,
              color: 'var(--color-accent, #c17838)',
            }}
          >
            {formatService.formatMoney(product.unitPrice || 0)}
          </div>
          <button
            type="button"
            className="btn btn-sm btn-primary"
            onClick={() => setShowModal(true)}
            disabled={!product.status}
            style={{
              padding: '6px 14px',
              borderRadius: '8px',
              fontSize: '0.85rem',
              fontWeight: 500,
            }}
          >
            {product.status ? 'Đặt món' : 'Tạm hết'}
          </button>
        </div>
      </div>

      {/* Popup / Card chọn Size, Topping và Đặt món */}
      {showModal && (
        <ProductOrderModal
          product={product}
          isOpen={showModal}
          onClose={() => setShowModal(false)}
          onNavigate={onNavigate}
        />
      )}
    </>
  );
};

export default ProductCard;
