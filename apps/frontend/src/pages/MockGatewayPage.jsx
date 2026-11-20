import React, { useEffect, useRef, useState } from 'react';
import { axiosClient, orderApi } from '../api';
import { formatService } from '../services/formatService';

export const MockGatewayPage = () => {
  const params = new URLSearchParams(window.location.search);
  const orderIdParam = params.get('orderId');
  const orderCodeParam = params.get('orderCode');
  const amountParam = params.get('amount');
  const methodParam = params.get('method') || 'VNPay';
  const refParam = params.get('ref') || `PAY-QR-${orderIdParam || Date.now()}`;

  const [order, setOrder] = useState(null);
  const [loadingOrder, setLoadingOrder] = useState(Boolean(orderIdParam));
  const [submitting, setSubmitting] = useState(false);
  const [resultStatus, setResultStatus] = useState('');
  const [statusMessage, setStatusMessage] = useState('');
  const [copiedKey, setCopiedKey] = useState('');
  const [nowTick, setNowTick] = useState(Date.now());
  const isSubmittingRef = useRef(false);

  useEffect(() => {
    const timer = setInterval(() => setNowTick(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!orderIdParam) {
      setLoadingOrder(false);
      return;
    }
    orderApi
      .getById(orderIdParam)
      .then((res) => {
        setOrder(res);
        if (res?.status === 'PAID') {
          setResultStatus('PAID');
          setStatusMessage('Đơn hàng này đã được thanh toán thành công.');
        } else if (res?.status === 'CANCELLED' || res?.status === 'PAYMENT_FAILED') {
          setResultStatus('FAILED');
          setStatusMessage('Đơn hàng đã bị hủy hoặc đã hết hạn thanh toán.');
        }
      })
      .catch(() => {})
      .finally(() => setLoadingOrder(false));
  }, [orderIdParam]);

  const effectiveMethod = order?.paymentMethod || methodParam;
  const channel = formatService.getPaymentChannelInfo(effectiveMethod);
  const displayOrderCode = order?.orderCode || orderCodeParam || (orderIdParam ? `BL${orderIdParam}` : 'N/A');
  const displayAmount = order?.totalAmount ?? (Number(amountParam) || 0);
  const remainingSeconds = order?.createdAt
    ? formatService.getOrderRemainingSeconds(order.createdAt, nowTick)
    : 300;
  const isExpired = Boolean(order?.createdAt && remainingSeconds <= 0 && resultStatus !== 'PAID');

  const handleCopy = (key, value) => {
    if (!value || !navigator?.clipboard) return;
    navigator.clipboard.writeText(String(value)).then(() => {
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(''), 1500);
    }).catch(() => {});
  };

  const handleConfirmPayment = async () => {
    if (!orderIdParam || isSubmittingRef.current || submitting) return;
    isSubmittingRef.current = true;
    setSubmitting(true);
    setStatusMessage('');

    try {
      const res = await axiosClient.get('/mock-gateway/pay', {
        params: {
          orderId: orderIdParam,
          method: effectiveMethod,
          ref: refParam,
        },
      });
      const simulatedResult = res?.simulatedResult || res?.status || 'SUCCESS';

      if (simulatedResult === 'SUCCESS') {
        const updatedOrder = await orderApi.confirmOrder(orderIdParam);
        setOrder(updatedOrder);
        setResultStatus('PAID');
        setStatusMessage('Thanh toán thành công! Trạng thái đơn hàng đã được cập nhật.');
      } else {
        try {
          await orderApi.cancel(orderIdParam, 'Thanh toán thất bại');
        } catch {}
        setResultStatus('FAILED');
        setStatusMessage('Giao dịch không thành công (tỷ lệ giả lập 40%). Đơn hàng đã bị hủy.');
      }
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Không thể xác nhận thanh toán. Vui lòng kiểm tra lại đơn hàng.';
      setResultStatus('FAILED');
      setStatusMessage(msg);
    } finally {
      setSubmitting(false);
      isSubmittingRef.current = false;
    }
  };

  const formatCountdown = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: '#f1f5f9',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px 14px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '440px',
          background: '#ffffff',
          borderRadius: '16px',
          overflow: 'hidden',
          border: '1px solid #cbd5e1',
          boxShadow: '0 16px 32px rgba(15, 23, 42, 0.1)',
        }}
      >
        <div
          style={{
            background: channel.headerColor,
            color: '#ffffff',
            padding: '18px 22px',
          }}
        >
          <div style={{ fontSize: '0.78rem', opacity: 0.85, fontWeight: 600, letterSpacing: '0.4px' }}>
            CỔNG THANH TOÁN BREWLITE COFFEE &amp; TEA
          </div>
          <div style={{ fontSize: '1.2rem', fontWeight: 800, marginTop: '4px' }}>
            {channel.bankName}
          </div>
          <div style={{ fontSize: '0.85rem', opacity: 0.92, marginTop: '2px' }}>
            {channel.accountName.toUpperCase()}
          </div>
        </div>

        <div style={{ padding: '22px' }}>
          {!orderIdParam ? (
            <div style={{ textAlign: 'center', color: '#dc2626', fontWeight: 600, padding: '20px 0' }}>
              Thiếu thông tin mã đơn hàng trên đường dẫn thanh toán.
            </div>
          ) : loadingOrder ? (
            <div style={{ textAlign: 'center', color: '#64748b', padding: '24px 0' }}>
              Đang tải thông tin thanh toán...
            </div>
          ) : (
            <>
              {resultStatus === '' && !isExpired && (
                <div
                  style={{
                    background: remainingSeconds <= 60 ? '#fef2f2' : '#fffbeb',
                    border: `1px solid ${remainingSeconds <= 60 ? '#fca5a5' : '#fde68a'}`,
                    borderRadius: '10px',
                    padding: '10px 14px',
                    marginBottom: '18px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontSize: '0.88rem',
                    fontWeight: 600,
                    color: remainingSeconds <= 60 ? '#b91c1c' : '#92400e',
                  }}
                >
                  <span>Thời gian giữ đơn còn lại</span>
                  <span style={{ fontFamily: 'monospace', fontSize: '1.1rem', fontWeight: 800 }}>
                    {formatCountdown(remainingSeconds)}
                  </span>
                </div>
              )}

              <div
                style={{
                  border: '1.5px dashed #fca5a5',
                  background: '#fff8f8',
                  borderRadius: '10px',
                  padding: '14px',
                  textAlign: 'center',
                  marginBottom: '18px',
                }}
              >
                <div style={{ fontSize: '0.74rem', fontWeight: 700, color: '#991b1b', letterSpacing: '0.5px', marginBottom: '4px' }}>
                  NỘI DUNG CHUYỂN KHOẢN
                </div>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', justifyContent: 'center' }}>
                  <span style={{ fontSize: '1.15rem', fontWeight: 800, color: '#dc2626' }}>
                    {displayOrderCode}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleCopy('code', displayOrderCode)}
                    style={{
                      padding: '2px 8px',
                      fontSize: '0.75rem',
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

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.92rem', marginBottom: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '10px', borderBottom: '1px solid #f1f5f9' }}>
                  <span style={{ color: '#64748b' }}>Đơn vị nhận</span>
                  <strong style={{ color: '#1e293b' }}>{channel.accountName}</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '10px', borderBottom: '1px solid #f1f5f9' }}>
                  <span style={{ color: '#64748b' }}>Ngân hàng</span>
                  <strong style={{ color: '#1e293b' }}>{channel.bankName}</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '10px', borderBottom: '1px solid #f1f5f9' }}>
                  <span style={{ color: '#64748b' }}>STK</span>
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
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ color: '#64748b' }}>Số tiền</span>
                  <strong style={{ color: 'var(--color-accent, #c17838)', fontSize: '1.15rem' }}>
                    {formatService.formatMoney(displayAmount)}
                  </strong>
                </div>
              </div>

              {statusMessage && (
                <div
                  style={{
                    padding: '12px 14px',
                    borderRadius: '10px',
                    marginBottom: '16px',
                    fontSize: '0.9rem',
                    fontWeight: 600,
                    textAlign: 'center',
                    background: resultStatus === 'PAID' ? '#dcfce7' : '#fee2e2',
                    color: resultStatus === 'PAID' ? '#15803d' : '#b91c1c',
                    border: resultStatus === 'PAID' ? '1px solid #86efac' : '1px solid #fecaca',
                  }}
                >
                  {statusMessage}
                </div>
              )}

              {resultStatus !== 'PAID' && resultStatus !== 'FAILED' && !isExpired ? (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleConfirmPayment}
                  disabled={submitting}
                  style={{
                    width: '100%',
                    padding: '13px',
                    fontSize: '0.96rem',
                    fontWeight: 700,
                    borderRadius: '10px',
                    cursor: submitting ? 'not-allowed' : 'pointer',
                  }}
                >
                  {submitting ? 'Đang xác nhận thanh toán...' : 'Tôi đã hoàn tất thanh toán'}
                </button>
              ) : isExpired && resultStatus === '' ? (
                <div
                  style={{
                    padding: '12px 14px',
                    borderRadius: '10px',
                    background: '#fee2e2',
                    color: '#b91c1c',
                    fontWeight: 600,
                    fontSize: '0.9rem',
                    textAlign: 'center',
                  }}
                >
                  Đơn hàng đã hết hạn thời gian thanh toán (quá 5 phút).
                </div>
              ) : null}

              <div style={{ marginTop: '14px', textAlign: 'center' }}>
                <a
                  href="/"
                  style={{ fontSize: '0.84rem', color: '#64748b', textDecoration: 'underline' }}
                >
                  Quay về trang chủ BrewLite Coffee &amp; Tea
                </a>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default MockGatewayPage;
