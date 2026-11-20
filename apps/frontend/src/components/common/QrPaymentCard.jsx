import React, { useState } from 'react';
import { formatService } from '../../services/formatService';

export const QrPaymentCard = ({
  orderId,
  orderCode,
  amount,
  method = 'VNPay',
  idempotencyKey,
  compact = false,
}) => {
  const [copiedKey, setCopiedKey] = useState('');
  const finalOrderCode = orderCode || `BL${orderId || ''}`;
  const channel = formatService.getPaymentChannelInfo(method);
  const gatewayUrl = formatService.buildGatewayUrl({
    orderId,
    orderCode: finalOrderCode,
    amount,
    method,
    ref: idempotencyKey || `PAY-${orderId || ''}`,
  });
  const qrSize = compact ? 165 : 195;
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(gatewayUrl)}`;

  const handleCopy = (key, value) => {
    if (!value || !navigator?.clipboard) return;
    navigator.clipboard.writeText(String(value)).then(() => {
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(''), 1500);
    }).catch(() => {});
  };

  return (
    <div
      style={{
        borderRadius: '14px',
        overflow: 'hidden',
        border: '1px solid #cbd5e1',
        background: '#ffffff',
        marginBottom: compact ? '16px' : '24px',
        textAlign: 'left',
        boxShadow: '0 4px 12px rgba(15, 23, 42, 0.06)',
      }}
    >
      <div
        style={{
          background: channel.headerColor,
          color: '#ffffff',
          padding: compact ? '12px 16px' : '14px 20px',
        }}
      >
        <div style={{ fontWeight: 800, fontSize: compact ? '1rem' : '1.08rem' }}>
          {channel.bankName}
        </div>
        <div style={{ fontSize: '0.8rem', opacity: 0.9, marginTop: '2px', letterSpacing: '0.3px' }}>
          {channel.accountName.toUpperCase()}
        </div>
      </div>

      <div style={{ padding: compact ? '16px' : '22px 20px' }}>
        <div style={{ textAlign: 'center', marginBottom: '16px' }}>
          <div
            style={{
              display: 'inline-block',
              padding: '10px',
              background: '#ffffff',
              borderRadius: '12px',
              border: '1px solid #e2e8f0',
              boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
            }}
          >
            <img
              src={qrUrl}
              alt="QR Thanh toán"
              style={{
                width: `${qrSize}px`,
                height: `${qrSize}px`,
                display: 'block',
              }}
            />
          </div>
          <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '8px' }}>
            Quét mã QR bằng điện thoại để mở trang xác nhận thanh toán
          </div>
        </div>

        <div
          style={{
            border: '1.5px dashed #fca5a5',
            background: '#fff8f8',
            borderRadius: '10px',
            padding: '12px 14px',
            textAlign: 'center',
            marginBottom: '14px',
          }}
        >
          <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#991b1b', letterSpacing: '0.5px', marginBottom: '4px' }}>
            NỘI DUNG CHUYỂN KHOẢN
          </div>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', justifyContent: 'center' }}>
            <span style={{ fontSize: compact ? '1rem' : '1.12rem', fontWeight: 800, color: '#dc2626' }}>
              {finalOrderCode}
            </span>
            <button
              type="button"
              onClick={() => handleCopy('code', finalOrderCode)}
              style={{
                padding: '2px 8px',
                fontSize: '0.72rem',
                fontWeight: 600,
                borderRadius: '5px',
                border: '1px solid #fecaca',
                background: '#ffffff',
                color: '#dc2626',
                cursor: 'pointer',
              }}
            >
              {copiedKey === 'code' ? 'Đã chép' : 'Sao chép'}
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '0.88rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '8px', borderBottom: '1px solid #f1f5f9' }}>
            <span style={{ color: '#64748b' }}>Số tiền</span>
            <strong style={{ color: 'var(--color-accent, #c17838)', fontSize: '1.02rem' }}>
              {formatService.formatMoney(amount || 0)}
            </strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '8px', borderBottom: '1px solid #f1f5f9' }}>
            <span style={{ color: '#64748b' }}>Số tài khoản</span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
              <strong style={{ color: '#059669' }}>{channel.accountNumber}</strong>
              <button
                type="button"
                onClick={() => handleCopy('stk', channel.accountNumber)}
                style={{
                  padding: '2px 7px',
                  fontSize: '0.72rem',
                  fontWeight: 600,
                  borderRadius: '5px',
                  border: '1px solid #e2e8f0',
                  background: '#f8fafc',
                  color: '#475569',
                  cursor: 'pointer',
                }}
              >
                {copiedKey === 'stk' ? 'Đã chép' : 'Sao chép'}
              </button>
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '8px', borderBottom: '1px solid #f1f5f9' }}>
            <span style={{ color: '#64748b' }}>Ngân hàng</span>
            <strong style={{ color: '#1e293b' }}>{channel.bankName}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ color: '#64748b' }}>Đơn vị nhận</span>
            <strong style={{ color: '#1e293b' }}>{channel.accountName}</strong>
          </div>
        </div>

        <div style={{ marginTop: '14px', textAlign: 'center', fontSize: '0.78rem', color: '#64748b' }}>
          Hệ thống đang chờ tín hiệu thanh toán và sẽ tự động cập nhật trạng thái...
        </div>
      </div>
    </div>
  );
};

export default QrPaymentCard;
