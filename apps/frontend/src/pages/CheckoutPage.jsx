import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { orderApi, paymentApi, cartApi, axiosClient } from '../api';
import { OrderType } from '../models/order.model';
import { formatService } from '../services/formatService';
import InteractiveButton from '../components/common/InteractiveButton';
import { IconArrowLeft } from '../components/common/Icons';
import ChiikawaMascot from '../components/common/ChiikawaMascot';

export const CheckoutPage = ({ onNavigate }) => {
  const { user } = useAuth();
  const { cartItems, subtotal, clearCart } = useCart();

  const [orderType, setOrderType] = useState(OrderType.DINE_IN);
  const [tableNumber, setTableNumber] = useState('');
  const [voucherCode, setVoucherCode] = useState('');
  const [voucherDetail, setVoucherDetail] = useState(null);
  const [discountAmount, setDiscountAmount] = useState(0);
  const [voucherSuccess, setVoucherSuccess] = useState('');
  const [voucherError, setVoucherError] = useState('');
  const [validatingVoucher, setValidatingVoucher] = useState(false);

  // Điểm thưởng tích lũy (Loyalty Points)
  const [availableLoyaltyPoints, setAvailableLoyaltyPoints] = useState(0);
  const [useLoyaltyPoints, setUseLoyaltyPoints] = useState(false);
  const [pointsToUse, setPointsToUse] = useState(0);

  const [paymentMethod, setPaymentMethod] = useState('VNPay'); // 'VNPay', 'Momo', 'Stripe' (chuẩn 100% backend Payment.Method)
  const [note, setNote] = useState('');

  const [loading, setLoading] = useState(false);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [createdOrder, setCreatedOrder] = useState(null);
  const [paymentData, setPaymentData] = useState(null);
  const [paymentSuccess, setPaymentSuccess] = useState(false);

  // Đồng bộ giỏ hàng từ Frontend lên Backend trước khi checkout
  const syncCartToServer = async (customerId) => {
    try {
      await cartApi.clearCart(customerId);
    } catch { }

    for (const item of cartItems) {
      try {
        const prodId = item.productId || item.id;
        const toppingsPayload = (item.toppings || []).map((t) => ({
          toppingId: t.id || t.toppingId,
          quantity: t.quantity || 1,
        }));

        await cartApi.addItem({
          customerId,
          item: {
            productId: prodId,
            quantity: item.quantity || 1,
            size: item.size || 'M',
            note: item.note || '',
            toppings: toppingsPayload,
          },
        });
      } catch (err) {
        console.warn('Lỗi thêm món giỏ server:', err);
      }
    }
  };

  // Tải thông tin điểm thưởng tích lũy của khách hàng
  useEffect(() => {
    const customerId = user?.id || 1;
    axiosClient
      .get(`/api/v1/customers/profile?userId=${customerId}`)
      .then((res) => {
        const pts = res?.loyaltyPoints ?? res?.loyaltyPoint ?? 0;
        setAvailableLoyaltyPoints(pts);
      })
      .catch(() => {
        setAvailableLoyaltyPoints(0);
      });
  }, [user]);

  // Xác thực và tính tiền giảm giá ngay lập tức khi gõ mã
  const validateVoucherCode = async (codeToTest) => {
    const code = (codeToTest ?? voucherCode).trim().toUpperCase();
    if (!code) {
      setDiscountAmount(0);
      setVoucherDetail(null);
      setVoucherSuccess('');
      setVoucherError('');
      return;
    }
    setValidatingVoucher(true);
    try {
      const res = await axiosClient.get('/api/promotions/validate', {
        params: { code, amount: subtotal },
      });
      const discount = Number(res?.discountAmount ?? res) || 0;
      let type = res?.discountType;
      let val = Number(res?.discountValue);

      // Nếu backend trả về số tiền thuần túy (chưa nạp DTO mới do chưa restart):
      if (!val) {
        try {
          const promoRes = await axiosClient.get('/api/promotions');
          const promoList = Array.isArray(promoRes) ? promoRes : promoRes?.data || promoRes?.value || [];
          const matched = promoList.find((p) => (p.code || '').toUpperCase() === code);
          if (matched) {
            type = matched.discountType;
            val = Number(matched.discountValue);
          }
        } catch {}
      }

      // Tự động suy luận % nếu có discount và subtotal
      if (!val && subtotal > 0 && discount > 0) {
        const ratio = Math.round((discount / subtotal) * 100);
        if (ratio > 0 && ratio <= 100) {
          type = 'PERCENT';
          val = ratio;
        }
      }

      const finalType = type || 'PERCENT';
      const finalVal = val || 20;

      setDiscountAmount(discount);
      setVoucherDetail({ code, discountType: finalType, discountValue: finalVal });

      const descType =
        finalType === 'PERCENT'
          ? `Giảm ${finalVal}%`
          : `Giảm ${formatService.formatMoney(finalVal)}`;

      setVoucherSuccess(`Đã áp dụng mã ${code} (${descType}): -${formatService.formatMoney(discount)}`);
      setVoucherError('');
    } catch (err) {
      setDiscountAmount(0);
      setVoucherDetail(null);
      setVoucherSuccess('');
      const msg = err.response?.data?.message || err.message || 'Mã ưu đãi không hợp lệ hoặc chưa đủ điều kiện';
      setVoucherError(msg);
    } finally {
      setValidatingVoucher(false);
    }
  };

  // Tự động kiểm tra và trừ tiền ngay khi người dùng gõ mã voucher
  useEffect(() => {
    if (!voucherCode.trim()) {
      setDiscountAmount(0);
      setVoucherDetail(null);
      setVoucherSuccess('');
      setVoucherError('');
      return;
    }
    const timer = setTimeout(() => {
      validateVoucherCode(voucherCode);
    }, 450);
    return () => clearTimeout(timer);
  }, [voucherCode, subtotal]);

  // Tính toán số tiền sau khi áp dụng Voucher và Điểm thưởng (1 điểm = 1.000đ)
  const subtotalAfterVoucher = Math.max(0, subtotal - discountAmount);
  const maxPointsAllowed = Math.min(
    availableLoyaltyPoints,
    Math.floor(subtotalAfterVoucher / 1000)
  );
  const actualPointsUsed = useLoyaltyPoints ? Math.min(pointsToUse, maxPointsAllowed) : 0;
  const pointsDiscount = actualPointsUsed * 1000;
  const finalTotal = Math.max(0, subtotalAfterVoucher - pointsDiscount);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (cartItems.length === 0) {
      setErrorMsg('Giỏ hàng của bạn đang trống. Vui lòng chọn món trước khi đặt.');
      return;
    }

    if (orderType === OrderType.DINE_IN && !tableNumber.trim()) {
      setErrorMsg('Vui lòng nhập số bàn để nhân viên phục vụ đúng chỗ.');
      return;
    }

    setLoading(true);

    try {
      const customerId = user?.id || 1;

      // 1. Đồng bộ dữ liệu giỏ hàng lên Backend để đảm bảo có giỏ hàng đang dùng
      await syncCartToServer(customerId);

      const payload = {
        customerId,
        orderType,
        tableNumber: orderType === OrderType.DINE_IN ? tableNumber.trim() : '',
        voucherCode: voucherCode.trim().toUpperCase() || undefined,
        loyaltyPointsUsed: actualPointsUsed,
        note: note.trim(),
        paymentMethod, // 'VNPay', 'Momo', hoặc 'Stripe'
      };

      // 2. Tạo đơn hàng qua checkoutService
      const orderRes = await orderApi.checkout(payload);
      setCreatedOrder(orderRes);
      clearCart();

      // 3. Yêu cầu cổng thanh toán theo đúng backend PaymentController (/api/v1/payments/request)
      const idempotencyKey = `PAY-${Date.now()}-${orderRes.id || 1}`;
      try {
        const payRes = await paymentApi.requestPayment({
          orderId: orderRes.id,
          method: paymentMethod, // 'VNPay', 'Momo', 'Stripe'
          idempotencyKey,
        });

        setPaymentData({
          orderId: orderRes.id,
          orderCode: orderRes.orderCode,
          amount: payRes.amount || orderRes.totalAmount || subtotal,
          method: payRes.method || paymentMethod,
          status: payRes.status || 'PENDING',
          paymentUrl: payRes.paymentUrl,
          qrCodeUrl: payRes.qrCodeUrl,
          idempotencyKey,
        });
      } catch (payErr) {
        console.warn('Lỗi khi gọi gateway thanh toán:', payErr);
        setPaymentData({
          orderId: orderRes.id,
          orderCode: orderRes.orderCode,
          amount: orderRes.totalAmount || subtotal,
          method: paymentMethod,
          status: 'PENDING',
          idempotencyKey,
        });
      }
    } catch (err) {
      console.error('Checkout error:', err);
      setErrorMsg(
        err.message || 'Không thể tạo đơn hàng. Vui lòng kiểm tra lại kết nối máy chủ.'
      );
    } finally {
      setLoading(false);
    }
  };

  // Xác nhận thanh toán qua Mock Gateway (chạy đúng tỷ lệ 60% SUCCESS / 40% FAILED)
  const handleConfirmPaymentSuccess = async () => {
    if (!paymentData?.orderId) return;
    setPaymentLoading(true);
    setErrorMsg('');
    try {
      // Gọi qua Mock Gateway để áp dụng đúng tỷ lệ 60% Thành công / 40% Thất bại
      const res = await axiosClient.get('/mock-gateway/pay', {
        params: {
          orderId: paymentData.orderId,
          method: paymentData.method,
          ref: paymentData.idempotencyKey,
        },
      });

      const simulatedResult = res?.simulatedResult || res?.status || 'SUCCESS';

      if (simulatedResult === 'SUCCESS') {
        setPaymentSuccess(true);
        setPaymentData((prev) => (prev ? { ...prev, status: 'PAID' } : null));
      } else {
        setPaymentSuccess(false);
        setPaymentData((prev) => (prev ? { ...prev, status: 'FAILED' } : null));
        setErrorMsg('Giao dịch thanh toán không thành công (Cổng thanh toán phản hồi: Thất bại / Thẻ không đủ số dư theo tỷ lệ giả lập 40%). Đơn hàng đã bị hủy.');
      }
    } catch (err) {
      console.error('Lỗi khi gọi cổng thanh toán giả lập:', err);
      setErrorMsg(err.message || 'Xác nhận thanh toán thất bại, vui lòng thử lại.');
    } finally {
      setPaymentLoading(false);
    }
  };

  // ==========================================
  // MÀN HÌNH SAU KHI ĐẶT HÀNG THÀNH CÔNG
  // ==========================================
  if (createdOrder) {
    const isPaid = paymentSuccess || paymentData?.status === 'PAID';
    const isFailed = paymentData?.status === 'FAILED';
    const isMomo = paymentData?.method === 'Momo';
    const isStripe = paymentData?.method === 'Stripe';
    const finalOrderCode = createdOrder.orderCode || `BL${createdOrder.id}`;
    const finalAmount = paymentData?.amount || createdOrder.totalAmount || subtotal;

    // Tạo mã QR chuyển khoản theo mã đơn hàng thực tế
    const qrData = isMomo
      ? `2|99|0987654321|BREWLITE COFFEE|brewlite@coffee.vn|0|0|${finalAmount}|${finalOrderCode}`
      : isStripe
        ? `STRIPE-PAYMENT-${finalOrderCode}-${finalAmount}`
        : `00020101021238540010A0000007270124000697042201121028749281920208QRIBFTTA5303704540${finalAmount}5802VN62${finalOrderCode.length + 4}08${finalOrderCode.length}${finalOrderCode}6304`;

    const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(
      qrData
    )}`;

    return (
      <div className="container" style={{ padding: '48px 24px 120px', maxWidth: '680px', margin: '0 auto' }}>
        <div
          style={{
            background: 'var(--color-surface)',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--color-border)',
            padding: '36px 32px',
            textAlign: 'center',
            boxShadow: 'var(--shadow-md)',
          }}
        >
          <h1
            style={{
              fontSize: '1.6rem',
              fontWeight: 800,
              marginBottom: '8px',
              color: isFailed ? '#dc2626' : 'inherit',
            }}
          >
            {isPaid
              ? 'Thanh toán thành công'
              : isFailed
                ? 'Thanh toán thất bại'
                : 'Đơn hàng đang chờ thanh toán'}
          </h1>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.95rem', marginBottom: '24px' }}>
            Mã đơn hàng: <strong style={{ color: 'var(--color-accent)' }}>{finalOrderCode}</strong>
          </p>

          <div
            style={{
              background: 'var(--color-surface-muted)',
              borderRadius: 'var(--radius-md)',
              padding: '20px',
              textAlign: 'left',
              marginBottom: '24px',
              fontSize: '0.92rem',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Hình thức nhận món:</span>
              <strong>{createdOrder.orderType === 'DINE_IN' ? `Tại bàn (${createdOrder.tableNumber || tableNumber})` : 'Mang đi'}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Cổng thanh toán:</span>
              <strong>{isMomo ? 'Ví MoMo' : isStripe ? 'Cổng Stripe Quốc tế' : 'VNPay / VietQR'}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Tổng thanh toán:</span>
              <strong style={{ color: 'var(--color-accent)', fontSize: '1.05rem' }}>
                {formatService.formatMoney(finalAmount)}
              </strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Trạng thái đơn:</span>
              <span
                style={{
                  padding: '4px 10px',
                  borderRadius: '6px',
                  fontSize: '0.82rem',
                  fontWeight: 700,
                  background: isPaid ? '#dcfce7' : isFailed ? '#fef2f2' : '#fef3c7',
                  color: isPaid ? '#15803d' : isFailed ? '#dc2626' : '#b45309',
                }}
              >
                {isPaid
                  ? 'ĐÃ THANH TOÁN (PAID)'
                  : isFailed
                    ? 'THANH TOÁN THẤT BẠI (FAILED)'
                    : 'CHỜ THANH TOÁN (PENDING)'}
              </span>
            </div>
          </div>

          {/* Trường hợp thanh toán thất bại */}
          {isFailed && (
            <div
              style={{
                border: '1px solid #fecaca',
                borderRadius: 'var(--radius-md)',
                padding: '24px',
                marginBottom: '24px',
                background: '#fef2f2',
                textAlign: 'center',
              }}
            >
              <div style={{ fontWeight: 700, fontSize: '1.05rem', color: '#991b1b', marginBottom: '8px' }}>
                Giao dịch không thành công
              </div>
              <p style={{ fontSize: '0.88rem', color: '#7f1d1d', margin: '0 0 16px', lineHeight: 1.5 }}>
                Cổng thanh toán đã mô phỏng giao dịch thất bại (theo tỷ lệ 40% giả lập). Đơn hàng đã được đánh dấu hủy trong hệ thống để đảm bảo dữ liệu.
              </p>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setCreatedOrder(null);
                  setPaymentData(null);
                  setPaymentSuccess(false);
                  setErrorMsg('');
                }}
                style={{ padding: '10px 20px', borderRadius: '8px' }}
              >
                Đặt lại đơn hàng mới
              </button>
            </div>
          )}

          {/* Phần quét mã QR thanh toán cổng */}
          {!isPaid && !isFailed && (
            <div
              style={{
                border: '1px dashed var(--color-border)',
                borderRadius: 'var(--radius-md)',
                padding: '24px',
                marginBottom: '24px',
                background: '#fafafa',
              }}
            >
              <div style={{ fontWeight: 700, fontSize: '1rem', marginBottom: '12px' }}>
                Quét mã để thanh toán cho đơn hàng {finalOrderCode}
              </div>
              <img
                src={qrUrl}
                alt="QR Thanh toán"
                style={{
                  width: '180px',
                  height: '180px',
                  borderRadius: '12px',
                  background: '#ffffff',
                  padding: '8px',
                  boxShadow: 'var(--shadow-sm)',
                  margin: '0 auto 14px',
                  display: 'block',
                }}
              />
              <div style={{ fontSize: '0.88rem', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
                <div>Đơn vị nhận: <strong>BrewLite Coffee</strong></div>
                <div>Kênh: <strong>{isMomo ? 'Ví điện tử MoMo' : isStripe ? 'Cổng quốc tế Stripe' : 'VietinBank (102874928192)'}</strong></div>
                <div>Nội dung chuyển khoản: <strong style={{ color: 'var(--color-accent)' }}>{finalOrderCode}</strong></div>
              </div>

              {paymentData?.paymentUrl && (
                <div style={{ marginTop: '12px' }}>
                  <a
                    href={paymentData.paymentUrl}
                    target="_blank"
                    rel="noreferrer"
                    style={{ fontSize: '0.85rem', color: '#2563eb', textDecoration: 'underline' }}
                  >
                    Mở cổng thanh toán trực tiếp
                  </a>
                </div>
              )}

              <div style={{ marginTop: '20px' }}>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleConfirmPaymentSuccess}
                  disabled={paymentLoading}
                  style={{ width: '100%', padding: '12px', fontSize: '0.95rem' }}
                >
                  {paymentLoading ? 'Đang xác nhận với hệ thống...' : 'Tôi đã hoàn tất thanh toán'}
                </button>
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'center', gap: '14px', flexWrap: 'wrap' }}>
            <button className="btn btn-primary" onClick={() => onNavigate('orders')}>
              Theo dõi đơn hàng của tôi
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setCreatedOrder(null);
                setPaymentData(null);
                onNavigate('menu');
              }}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <IconArrowLeft size={16} />
              <span>Quay lại chọn thêm món</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ==========================================
  // MÀN HÌNH ĐẶT HÀNG (CHECKOUT FORM)
  // ==========================================
  return (
    <div className="container" style={{ padding: '28px 24px 60px' }}>
      <button
        type="button"
        onClick={() => {
          if (onNavigate) onNavigate('cart');
          else window.history.back();
        }}
        className="btn btn-secondary btn-sm"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '8px',
          padding: '8px 14px',
          borderRadius: '8px',
          marginBottom: '16px',
          fontWeight: 600,
          fontSize: '0.88rem',
          cursor: 'pointer',
          border: '1px solid var(--color-border)',
        }}
      >
        <IconArrowLeft size={18} />
        <span>Quay lại chỉnh sửa món</span>
      </button>

      <h1 style={{ fontSize: '1.8rem', fontWeight: 800, marginBottom: '20px' }}>
        Xác nhận và đặt hàng
      </h1>

      {/* Bé mascot Chiikawa mách nước voucher giảm giá */}
      <ChiikawaMascot
        character="usagi"
        message="Bạn có thể sử dụng KHAITRUONG để được giảm 20% cho tổng đơn hàng"
        actionText="Áp dụng ngay"
        onActionClick={() => {
          setVoucherCode('KHAITRUONG');
          validateVoucherCode('KHAITRUONG');
        }}
      />

      {errorMsg && (
        <div
          style={{
            padding: '12px 16px',
            background: '#fee2e2',
            color: '#b91c1c',
            borderRadius: 'var(--radius-md)',
            marginBottom: '20px',
            fontWeight: 600,
            fontSize: '0.92rem',
          }}
        >
          {errorMsg}
        </div>
      )}

      <form onSubmit={handleSubmit} className="cart-page-grid">
        {/* CỘT TRÁI: THÔNG TIN NHẬN MÓN VÀ PHƯƠNG THỨC THANH TOÁN */}
        <div
          style={{
            background: 'var(--color-surface)',
            padding: '24px',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--color-border)',
          }}
        >
          <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: '18px' }}>
            1. Thông tin nhận đồ uống
          </h3>

          <div className="form-group">
            <label className="form-label">Hình thức dùng món</label>
            <div style={{ display: 'flex', gap: '12px' }}>
              {[
                { type: OrderType.DINE_IN, label: 'Dùng tại bàn' },
                { type: OrderType.TAKE_AWAY, label: 'Mang đi' },
              ].map((opt) => (
                <button
                  type="button"
                  key={opt.type}
                  onClick={() => setOrderType(opt.type)}
                  style={{
                    flex: 1,
                    padding: '12px',
                    borderRadius: 'var(--radius-md)',
                    border: '1.5px solid',
                    borderColor:
                      orderType === opt.type
                        ? 'var(--color-accent)'
                        : 'var(--color-border)',
                    background:
                      orderType === opt.type
                        ? 'rgba(193, 120, 56, 0.08)'
                        : 'var(--color-surface)',
                    fontWeight: orderType === opt.type ? 700 : 500,
                    cursor: 'pointer',
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {orderType === OrderType.DINE_IN && (
            <div className="form-group">
              <label className="form-label">Số bàn của bạn *</label>
              <input
                type="text"
                className="form-input"
                placeholder="VD: Bàn 04"
                value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                required
              />
            </div>
          )}

          <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginTop: '28px', marginBottom: '18px' }}>
            2. Phương thức thanh toán
          </h3>

          <div className="form-group">
            <div style={{ display: 'flex', gap: '10px', flexDirection: 'column' }}>
              {[
                { id: 'VNPay', label: 'Chuyển khoản QR / VNPay Banking', desc: 'Quét mã VietQR bằng ứng dụng ngân hàng' },
                { id: 'Momo', label: 'Ví điện tử MoMo', desc: 'Thanh toán qua ứng dụng ví MoMo' },
                { id: 'Stripe', label: 'Cổng thanh toán Stripe', desc: 'Hỗ trợ thẻ quốc tế Visa / MasterCard' },
              ].map((m) => (
                <label
                  key={m.id}
                  onClick={() => setPaymentMethod(m.id)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    padding: '12px 16px',
                    borderRadius: 'var(--radius-md)',
                    border: '1.5px solid',
                    borderColor: paymentMethod === m.id ? 'var(--color-accent)' : 'var(--color-border)',
                    background: paymentMethod === m.id ? 'rgba(193, 120, 56, 0.05)' : 'var(--color-surface)',
                    cursor: 'pointer',
                  }}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    checked={paymentMethod === m.id}
                    onChange={() => setPaymentMethod(m.id)}
                    style={{ accentColor: 'var(--color-accent)', width: '16px', height: '16px' }}
                  />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.92rem' }}>{m.label}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)' }}>{m.desc}</div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginTop: '28px', marginBottom: '18px' }}>
            3. Thông tin bổ sung
          </h3>

          <div className="form-group">
            <label className="form-label">Mã ưu đãi / Voucher (Nếu có)</label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input
                type="text"
                className="form-input"
                placeholder="VD: KHAITRUONG, GIAM20K"
                value={voucherCode}
                onChange={(e) => setVoucherCode(e.target.value.toUpperCase())}
                style={{ flex: 1, textTransform: 'uppercase' }}
              />
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => validateVoucherCode(voucherCode)}
                disabled={validatingVoucher || !voucherCode.trim()}
                style={{ padding: '8px 16px', fontWeight: 600, whiteSpace: 'nowrap' }}
              >
                {validatingVoucher ? 'Đang kiểm tra...' : 'Áp dụng'}
              </button>
            </div>
            {voucherSuccess && (
              <div style={{ color: '#16a34a', fontSize: '0.84rem', marginTop: '6px', fontWeight: 600 }}>
                ✓ {voucherSuccess}
              </div>
            )}
            {voucherError && (
              <div style={{ color: '#dc2626', fontSize: '0.84rem', marginTop: '6px' }}>
                ✕ {voucherError}
              </div>
            )}
          </div>

          <div className="form-group">
            <label className="form-label">Ghi chú cho quầy pha chế</label>
            <textarea
              className="form-input"
              rows={2}
              placeholder="VD: Ít ngọt, nhiều đá, để riêng đá..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>

          {/* 4. ĐIỂM THƯỞNG TÍCH LŨY (LOYALTY POINTS) */}
          <div style={{ marginTop: '24px', padding: '16px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#1e293b' }}>
                  Điểm thưởng tích lũy (Loyalty Points)
                </div>
                <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '2px' }}>
                  Bạn hiện có: <strong style={{ color: '#2563eb', fontSize: '0.9rem' }}>{availableLoyaltyPoints}</strong> điểm (Tỷ lệ quy đổi: 1 điểm = 1.000 đ)
                </div>
              </div>
              {availableLoyaltyPoints > 0 && maxPointsAllowed > 0 && (
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.88rem', fontWeight: 600 }}>
                  <input
                    type="checkbox"
                    checked={useLoyaltyPoints}
                    onChange={(e) => {
                      setUseLoyaltyPoints(e.target.checked);
                      if (e.target.checked && pointsToUse <= 0) {
                        setPointsToUse(maxPointsAllowed);
                      }
                    }}
                    style={{ width: '16px', height: '16px', accentColor: '#2563eb', cursor: 'pointer' }}
                  />
                  <span>Dùng điểm giảm giá</span>
                </label>
              )}
            </div>

            {useLoyaltyPoints && availableLoyaltyPoints > 0 && (
              <div style={{ marginTop: '12px', paddingTop: '12px', borderTop: '1px dashed #cbd5e1', display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <label style={{ fontSize: '0.85rem', color: '#475569' }}>Số điểm dùng:</label>
                  <input
                    type="number"
                    min="1"
                    max={maxPointsAllowed}
                    value={pointsToUse}
                    onChange={(e) => setPointsToUse(Math.max(0, Math.min(maxPointsAllowed, parseInt(e.target.value) || 0)))}
                    className="form-input"
                    style={{ width: '100px', padding: '6px 10px', fontSize: '0.9rem' }}
                  />
                </div>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setPointsToUse(maxPointsAllowed)}
                  style={{ padding: '6px 12px', fontSize: '0.82rem' }}
                >
                  Dùng tối đa ({maxPointsAllowed} điểm)
                </button>
                <div style={{ fontSize: '0.88rem', color: '#16a34a', fontWeight: 700 }}>
                  Giảm -{formatService.formatMoney(actualPointsUsed * 1000)}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* CỘT PHẢI: BOX ORDER TÓM TẮT ĐƠN HÀNG */}
        <div>
          <div className="order-summary-card">
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '18px' }}>
              Món trong đơn ({cartItems.length})
            </h3>

            <div style={{ maxHeight: '280px', overflowY: 'auto', marginBottom: '18px' }}>
              {cartItems.map((item) => (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    padding: '10px 0',
                    borderBottom: '1px solid var(--color-border)',
                  }}
                >
                  <div style={{ paddingRight: '8px' }}>
                    <div style={{ fontSize: '0.92rem', fontWeight: 600 }}>
                      {item.name} <span style={{ color: 'var(--color-accent)' }}>x {item.quantity}</span>
                    </div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                      Size {item.size} {item.toppings?.length > 0 ? `• Topping: ${item.toppings.map((t) => t.name).join(', ')}` : ''}
                    </div>
                    {item.note && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', fontStyle: 'italic' }}>
                        Ghi chú: {item.note}
                      </div>
                    )}
                  </div>
                  <div style={{ fontWeight: 700, fontSize: '0.92rem', whiteSpace: 'nowrap' }}>
                    {formatService.formatMoney(item.itemTotalPrice || item.unitPrice * item.quantity)}
                  </div>
                </div>
              ))}
            </div>

            <div className="summary-row">
              <span style={{ color: 'var(--color-text-secondary)' }}>Tạm tính</span>
              <span style={{ fontWeight: 700 }}>{formatService.formatMoney(subtotal)}</span>
            </div>

            {discountAmount > 0 && (
              <div className="summary-row" style={{ color: '#16a34a' }}>
                <span>
                  Giảm giá ({voucherCode.trim().toUpperCase()}
                  {voucherDetail?.discountType === 'PERCENT'
                    ? ` - Giảm ${Number(voucherDetail.discountValue)}%`
                    : voucherDetail?.discountType === 'FIXED'
                    ? ` - Giảm ${formatService.formatMoney(voucherDetail.discountValue)}`
                    : ''}
                  )
                </span>
                <span style={{ fontWeight: 700 }}>-{formatService.formatMoney(discountAmount)}</span>
              </div>
            )}

            {pointsDiscount > 0 && (
              <div className="summary-row" style={{ color: '#2563eb' }}>
                <span>Điểm thưởng ({actualPointsUsed} điểm)</span>
                <span style={{ fontWeight: 700 }}>-{formatService.formatMoney(pointsDiscount)}</span>
              </div>
            )}

            <div className="summary-row total">
              <span>Tổng thanh toán</span>
              <span style={{ color: 'var(--color-accent)' }}>
                {formatService.formatMoney(finalTotal)}
              </span>
            </div>

            <InteractiveButton
              text="Xác nhận đặt hàng"
              loadingText="Đang xử lý đơn..."
              successText="Đặt hàng thành công!"
              loading={loading}
              success={Boolean(createdOrder)}
              type="submit"
              style={{ marginTop: '20px', padding: '14px' }}
            />
          </div>
        </div>
      </form>
    </div>
  );
};

export default CheckoutPage;
