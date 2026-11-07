import React, { useEffect, useState, useRef } from 'react';
import { orderApi } from '../api';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { formatService } from '../services/formatService';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { resolveProductImage, defaultCoffeeImg } from '../images';

export const OrderHistoryPage = ({ onNavigate }) => {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const pollingRef = useRef(null);

  // State cho Modal xác nhận hủy đơn (Mục 4)
  const [orderToCancel, setOrderToCancel] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  // State cho Modal xem chi tiết đơn hàng (Mục 5)
  const [selectedOrderDetail, setSelectedOrderDetail] = useState(null);

  // State cho Modal tiếp tục thanh toán (Mục 5)
  const [paymentModalOrder, setPaymentModalOrder] = useState(null);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentMsg, setPaymentMsg] = useState('');

  const loadOrders = async (isBackground = false) => {
    try {
      if (!isBackground) setLoading(true);
      const customerId = user?.customerId || user?.id || 1;
      const res = await orderApi.getHistory({ customerId, page: 0, size: 30 });
      const list = res?.content || (Array.isArray(res) ? res : []);
      // Tự động bổ sung chi tiết items cho từng đơn hàng nếu danh sách tóm tắt chưa kèm items
      const enrichedList = await Promise.all(
        list.map(async (o) => {
          if (!o.items || o.items.length === 0) {
            try {
              const detail = await orderApi.getById(o.id);
              if (detail?.items) {
                return {
                  ...o,
                  items: detail.items,
                  orderType: detail.orderType || o.orderType,
                  tableNumber: detail.tableNumber || o.tableNumber,
                };
              }
            } catch {}
          }
          return o;
        })
      );
      setOrders(enrichedList);
    } catch {
      if (!isBackground) setOrders([]);
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();

    // Tự động làm mới mỗi 5 giây để nhận trạng thái mới theo thời gian thực
    pollingRef.current = setInterval(() => {
      loadOrders(true);
    }, 5000);

    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [user]);

  // Mở popup chi tiết đơn và tự động tải dữ liệu đầy đủ nhất
  const handleOpenOrderDetail = async (order) => {
    setSelectedOrderDetail(order);
    try {
      const fullOrder = await orderApi.getById(order.id);
      if (fullOrder && fullOrder.items) {
        setSelectedOrderDetail(fullOrder);
      }
    } catch (err) {
      console.error('Không thể tải chi tiết đơn hàng:', err);
    }
  };

  // Thực hiện hủy đơn sau khi xác nhận trên Modal (Chỉ đơn PENDING chưa thanh toán)
  const confirmCancelOrder = async () => {
    if (!orderToCancel) return;
    setCancelling(true);
    try {
      await orderApi.cancel(orderToCancel.id, 'Khách hàng yêu cầu hủy qua ứng dụng');
      setOrderToCancel(null);
      await loadOrders();
    } catch (error) {
      alert(error.message || 'Hủy đơn thất bại. Vui lòng liên hệ nhân viên.');
    } finally {
      setCancelling(false);
    }
  };

  // Xử lý xác nhận thanh toán giả lập khi tiếp tục thanh toán từ Đơn Hàng Của Tôi
  const handlePayNow = async (order) => {
    setPaymentLoading(true);
    setPaymentMsg('');
    try {
      const res = await axiosClient.get('/mock-gateway/pay', {
        params: {
          orderId: order.id,
          method: 'VNPay',
          ref: `PAY-RESUME-${Date.now()}-${order.id}`,
        },
      });

      const simulatedResult = res?.simulatedResult || res?.status || 'SUCCESS';
      if (simulatedResult === 'SUCCESS') {
        setPaymentMsg('Thanh toán thành công! Trạng thái đơn đã được cập nhật.');
        setTimeout(() => {
          setPaymentModalOrder(null);
          setPaymentMsg('');
          loadOrders();
        }, 1200);
      } else {
        setPaymentMsg('Thanh toán không thành công theo xác suất thử nghiệm. Đơn hàng đã bị hủy.');
        setTimeout(() => {
          setPaymentModalOrder(null);
          setPaymentMsg('');
          loadOrders();
        }, 1800);
      }
    } catch (err) {
      setPaymentMsg(err.message || 'Xác nhận thanh toán thất bại.');
    } finally {
      setPaymentLoading(false);
    }
  };

  // Xác định bước tiến trình của đơn
  const getStepProgress = (status) => {
    switch (status) {
      case 'PENDING':
        return 1;
      case 'PAID':
        return 2;
      case 'PREPARING':
        return 3;
      case 'READY':
        return 4;
      case 'COMPLETED':
        return 5;
      default:
        return 0;
    }
  };

  if (loading) {
    return <LoadingSpinner text="Đang tải lịch sử đơn hàng..." />;
  }

  return (
    <div className="container" style={{ padding: '36px 24px 120px', maxWidth: '850px', margin: '0 auto' }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800 }}>Đơn Hàng Của Tôi</h1>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>
            Theo dõi trạng thái pha chế và nhận đồ uống theo thời gian thực (tự động cập nhật)
          </p>
        </div>

        <button className="btn btn-secondary btn-sm" onClick={() => loadOrders(false)}>
          Làm mới
        </button>
      </div>

      {orders.length === 0 ? (
        <div
          style={{
            textAlign: 'center',
            padding: '60px 20px',
            background: 'var(--color-surface)',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--color-border)',
          }}
        >
          <h3 style={{ fontSize: '1.25rem', marginBottom: '8px' }}>Chưa có đơn hàng nào</h3>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: '20px', fontSize: '0.92rem' }}>
            Hãy chọn một ly đồ uống thơm ngon từ thực đơn của BrewLite ngay hôm nay!
          </p>
          <button className="btn btn-primary" onClick={() => onNavigate('menu')}>
            Khám phá thực đơn
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {orders.map((order) => {
            const statusInfo = formatService.getOrderStatusInfo(order.status);
            const currentStep = getStepProgress(order.status);
            const isCancelled = order.status === 'CANCELLED' || order.status === 'PAYMENT_FAILED';
            const isPending = order.status === 'PENDING';

            return (
              <div
                key={order.id}
                style={{
                  background: 'var(--color-surface, #ffffff)',
                  padding: '24px',
                  borderRadius: '16px',
                  border: '1px solid var(--color-border, #e2e8f0)',
                  boxShadow: 'var(--shadow-sm)',
                  transition: 'box-shadow 0.2s',
                }}
              >
                {/* Header đơn hàng */}
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    borderBottom: '1px solid var(--color-surface-muted, #f1f5f9)',
                    paddingBottom: '14px',
                    marginBottom: '16px',
                    flexWrap: 'wrap',
                    gap: '10px',
                  }}
                >
                  <div
                    style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
                    onClick={() => handleOpenOrderDetail(order)}
                    title="Bấm để xem chi tiết đơn hàng"
                  >
                    <span style={{ fontWeight: 800, fontSize: '1.15rem', color: 'var(--color-accent, #c17838)' }}>
                      {order.orderCode || `#BL${order.id}`}
                    </span>
                    <span
                      style={{
                        marginLeft: '12px',
                        fontSize: '0.85rem',
                        color: 'var(--color-text-muted, #64748b)',
                      }}
                    >
                      {formatService.formatDateTime(order.createdAt)}
                    </span>
                    <span style={{ marginLeft: '8px', fontSize: '0.78rem', color: '#2563eb', textDecoration: 'underline' }}>
                      (Xem chi tiết)
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span
                      className="status-badge"
                      style={{
                        background: statusInfo.bg,
                        color: statusInfo.color,
                        padding: '4px 10px',
                        borderRadius: '6px',
                        fontWeight: 600,
                        fontSize: '0.85rem',
                      }}
                    >
                      {statusInfo.label}
                    </span>

                    {/* Nút Tiếp tục thanh toán nếu đang Chờ thanh toán */}
                    {isPending && (
                      <button
                        className="btn btn-primary btn-sm"
                        style={{ padding: '4px 10px', fontSize: '0.82rem', fontWeight: 600 }}
                        onClick={() => setPaymentModalOrder(order)}
                      >
                        Tiếp tục thanh toán
                      </button>
                    )}

                    {/* Nút Hủy đơn: Chỉ cho phép hủy khi đang Chờ thanh toán (PENDING). Đã thanh toán không cho hủy */}
                    {isPending && (
                      <button
                        className="btn btn-secondary btn-sm"
                        style={{ color: '#dc2626', borderColor: '#fca5a5', padding: '4px 10px', fontSize: '0.82rem' }}
                        onClick={() => setOrderToCancel(order)}
                      >
                        Hủy đơn
                      </button>
                    )}
                  </div>
                </div>

                {/* Thanh tiến trình thời gian thực */}
                {!isCancelled && (
                  <div
                    style={{
                      background: 'var(--color-surface-muted, #f8fafc)',
                      borderRadius: '8px',
                      padding: '14px 12px',
                      marginBottom: '16px',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', position: 'relative' }}>
                      {[
                        { step: 1, label: 'Đã nhận đơn' },
                        { step: 2, label: 'Đã thanh toán' },
                        { step: 3, label: 'Barista đang pha' },
                        { step: 4, label: 'Mời nhận món' },
                        { step: 5, label: 'Hoàn tất' },
                      ].map((s) => {
                        const isActive = currentStep >= s.step;
                        const isCurrent = currentStep === s.step;
                        return (
                          <div
                            key={s.step}
                            style={{
                              flex: 1,
                              textAlign: 'center',
                              position: 'relative',
                            }}
                          >
                            <div
                              style={{
                                width: '24px',
                                height: '24px',
                                borderRadius: '50%',
                                background: isActive ? 'var(--color-accent, #c17838)' : '#cbd5e1',
                                color: '#ffffff',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                margin: '0 auto 6px',
                                boxShadow: isCurrent ? '0 0 0 3px rgba(193, 120, 56, 0.25)' : 'none',
                              }}
                            >
                              {s.step}
                            </div>
                            <div
                              style={{
                                fontSize: '0.75rem',
                                fontWeight: isCurrent ? 600 : 400,
                                color: isActive ? 'var(--color-text-main)' : 'var(--color-text-muted)',
                              }}
                            >
                              {s.label}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Danh sách món trong đơn (Hiển thị trực tiếp trên card theo Mục 5) */}
                <div style={{ marginBottom: '14px' }}>
                  {order.items && order.items.length > 0 ? (
                    order.items.map((item, idx) => (
                      <div
                        key={idx}
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          padding: '8px 0',
                          borderBottom:
                            idx < order.items.length - 1
                              ? '1px solid var(--color-surface-muted, #f1f5f9)'
                              : 'none',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          {item.productImage && (
                            <img
                              src={item.productImage}
                              alt={item.productName}
                              style={{ width: '40px', height: '40px', borderRadius: '6px', objectFit: 'cover' }}
                              onError={(e) => {
                                e.target.src = defaultCoffeeImg;
                              }}
                            />
                          )}
                          <div>
                            <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>
                              {item.productName || item.name} <span style={{ color: '#64748b' }}>x {item.quantity}</span>
                            </div>
                            <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
                              Size {item.size || 'M'}
                              {item.toppings && item.toppings.length > 0 && (
                                <span> • Topping: {item.toppings.map((t) => t.toppingName || t.name || t).join(', ')}</span>
                              )}
                            </div>
                          </div>
                        </div>

                        <div style={{ fontWeight: 600, fontSize: '0.92rem', color: 'var(--color-accent, #c17838)' }}>
                          {formatService.formatMoney(item.lineTotal || (item.unitPrice || 0) * (item.quantity || 1))}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div style={{ fontSize: '0.88rem', color: '#64748b', fontStyle: 'italic', padding: '6px 0' }}>
                      Tổng số lượng món: {order.totalItems || 1} món
                    </div>
                  )}
                </div>

                {/* Footer đơn hàng */}
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    paddingTop: '12px',
                    borderTop: '1px dashed var(--color-border, #e2e8f0)',
                  }}
                >
                  <span style={{ fontSize: '0.88rem', color: 'var(--color-text-secondary)' }}>
                    Hình thức:{' '}
                    <strong>
                      {order.orderType === 'DINE_IN'
                        ? `Tại bàn (${order.tableNumber || 'Chưa chọn số bàn'})`
                        : 'Mang đi'}
                    </strong>
                  </span>
                  <div style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--color-accent, #c17838)' }}>
                    Tổng cộng: {formatService.formatMoney(order.totalAmount || order.subtotal)}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ======================================================== */}
      {/* MODAL 1: XÁC NHẬN HỦY ĐƠN HÀNG (Mục 4 - In đỏ nút Hủy)    */}
      {/* ======================================================== */}
      {orderToCancel && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.55)',
            backdropFilter: 'blur(3px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1050,
            padding: '16px',
          }}
          onClick={() => setOrderToCancel(null)}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '420px',
              width: '100%',
              padding: '28px 24px',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
              border: '1px solid #e2e8f0',
              textAlign: 'center',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              style={{
                width: '52px',
                height: '52px',
                borderRadius: '50%',
                background: '#fee2e2',
                color: '#dc2626',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '1.6rem',
                margin: '0 auto 16px',
              }}
            >
              ⚠
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 8px', color: '#1e293b' }}>
              Xác nhận hủy đơn hàng?
            </h3>
            <p style={{ color: '#64748b', fontSize: '0.92rem', margin: '0 0 24px', lineHeight: 1.5 }}>
              Bạn có chắc muốn hủy đơn hàng <strong>{orderToCancel.orderCode || `#BL${orderToCancel.id}`}</strong> không? Hành động này không thể hoàn tác.
            </p>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setOrderToCancel(null)}
                style={{ flex: 1, padding: '10px', borderRadius: '8px', fontWeight: 500 }}
              >
                Quay lại
              </button>
              {/* Nút Hủy được in màu đỏ rõ ràng theo đúng yêu cầu */}
              <button
                type="button"
                onClick={confirmCancelOrder}
                disabled={cancelling}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  background: '#dc2626',
                  color: '#ffffff',
                  border: 'none',
                  fontWeight: 700,
                  fontSize: '0.95rem',
                  cursor: 'pointer',
                  boxShadow: '0 4px 6px -1px rgba(220, 38, 38, 0.3)',
                }}
              >
                {cancelling ? 'Đang hủy...' : 'Hủy đơn'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================== */}
      {/* MODAL 2: XEM CHI TIẾT ĐƠN HÀNG (Mục 5 - Xử lý nhiều ảnh) */}
      {/* ======================================================== */}
      {selectedOrderDetail && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.55)',
            backdropFilter: 'blur(3px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1050,
            padding: '16px',
          }}
          onClick={() => setSelectedOrderDetail(null)}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '520px',
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              padding: '28px',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
              border: '1px solid #e2e8f0',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 style={{ fontSize: '1.3rem', fontWeight: 700, margin: 0 }}>
                Chi Tiết Đơn Hàng {selectedOrderDetail.orderCode || `#BL${selectedOrderDetail.id}`}
              </h2>
              <button
                onClick={() => setSelectedOrderDetail(null)}
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#64748b' }}
              >
                ✕
              </button>
            </div>

            <div style={{ background: '#f8fafc', padding: '14px', borderRadius: '10px', marginBottom: '18px', fontSize: '0.88rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                <span style={{ color: '#64748b' }}>Thời gian đặt:</span>
                <strong>{formatService.formatDateTime(selectedOrderDetail.createdAt)}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                <span style={{ color: '#64748b' }}>Trạng thái:</span>
                <strong>{formatService.getOrderStatusInfo(selectedOrderDetail.status).label}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#64748b' }}>Hình thức:</span>
                <strong>
                  {selectedOrderDetail.orderType === 'DINE_IN'
                    ? `Tại bàn (${selectedOrderDetail.tableNumber || 'Chưa rõ'})`
                    : 'Mang đi'}
                </strong>
              </div>
            </div>

            <h4 style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '10px' }}>Danh sách món đã đặt:</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
              {selectedOrderDetail.items && selectedOrderDetail.items.length > 0 ? (
                selectedOrderDetail.items.map((item, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px',
                      background: '#ffffff',
                      border: '1px solid #f1f5f9',
                      borderRadius: '8px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      {/* Xử lý thu nhỏ ảnh món ăn để nếu có nhiều món/nhiều ảnh vẫn gọn gàng */}
                      <img
                        src={item.productImage || defaultCoffeeImg}
                        alt={item.productName}
                        style={{
                          width: '48px',
                          height: '48px',
                          borderRadius: '8px',
                          objectFit: 'cover',
                          border: '1px solid #e2e8f0',
                          flexShrink: 0,
                        }}
                        onError={(e) => {
                          e.target.src = defaultCoffeeImg;
                        }}
                      />
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>
                          {item.productName || item.name} <span style={{ color: '#64748b' }}>x {item.quantity}</span>
                        </div>
                        <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '2px' }}>
                          Size: {item.size || 'M'}
                          {item.toppings && item.toppings.length > 0 && (
                            <div>Topping: {item.toppings.map((t) => t.name || t.toppingName || t).join(', ')}</div>
                          )}
                        </div>
                      </div>
                    </div>

                    <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--color-accent, #c17838)' }}>
                      {formatService.formatMoney(item.lineTotal || (item.unitPrice || 0) * (item.quantity || 1))}
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.9rem' }}>
                  Không có chi tiết từng món
                </div>
              )}
            </div>

            <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '14px', marginBottom: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.1rem', fontWeight: 700 }}>
                <span>Tổng thanh toán:</span>
                <span style={{ color: 'var(--color-accent, #c17838)' }}>
                  {formatService.formatMoney(selectedOrderDetail.totalAmount || selectedOrderDetail.subtotal)}
                </span>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              {selectedOrderDetail.status === 'PENDING' && (
                <button
                  type="button"
                  className="btn btn-primary"
                  style={{ flex: 1, padding: '10px' }}
                  onClick={() => {
                    setPaymentModalOrder(selectedOrderDetail);
                    setSelectedOrderDetail(null);
                  }}
                >
                  Thanh toán ngay
                </button>
              )}
              <button
                type="button"
                className="btn btn-secondary"
                style={{ flex: 1, padding: '10px' }}
                onClick={() => setSelectedOrderDetail(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================== */}
      {/* MODAL 3: TIẾP TỤC THANH TOÁN (Mục 5 & Mục 6)              */}
      {/* ======================================================== */}
      {paymentModalOrder && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.55)',
            backdropFilter: 'blur(3px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1050,
            padding: '16px',
          }}
          onClick={() => setPaymentModalOrder(null)}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '460px',
              width: '100%',
              padding: '28px 24px',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
              border: '1px solid #e2e8f0',
              textAlign: 'center',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ fontSize: '1.3rem', fontWeight: 700, margin: '0 0 6px' }}>
              Thanh Toán Đơn {paymentModalOrder.orderCode || `#BL${paymentModalOrder.id}`}
            </h3>
            <p style={{ color: '#64748b', fontSize: '0.88rem', margin: '0 0 16px' }}>
              Số tiền cần thanh toán: <strong style={{ color: 'var(--color-accent, #c17838)', fontSize: '1.1rem' }}>{formatService.formatMoney(paymentModalOrder.totalAmount)}</strong>
            </p>

            {/* Mã QR */}
            <div style={{ margin: '0 auto 16px', display: 'inline-block', padding: '8px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
              <img
                src={`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(
                  `00020101021238540010A0000007270124000697042201121028749281920208QRIBFTTA5303704540${paymentModalOrder.totalAmount}5802VN62${paymentModalOrder.orderCode ? paymentModalOrder.orderCode.length + 4 : 8}08${paymentModalOrder.orderCode ? paymentModalOrder.orderCode.length : 4}${paymentModalOrder.orderCode || 'BL' + paymentModalOrder.id}6304`
                )}`}
                alt="QR Code"
                style={{ width: '160px', height: '160px', display: 'block' }}
              />
            </div>

            <div style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '16px' }}>
              Ngân hàng: <strong>VietinBank (102874928192)</strong> • Nội dung: <strong>{paymentModalOrder.orderCode || `BL${paymentModalOrder.id}`}</strong>
            </div>

            {paymentMsg && (
              <div
                style={{
                  padding: '10px',
                  borderRadius: '8px',
                  marginBottom: '16px',
                  fontSize: '0.88rem',
                  fontWeight: 600,
                  background: paymentMsg.includes('thành công') ? '#dcfce7' : '#fee2e2',
                  color: paymentMsg.includes('thành công') ? '#15803d' : '#b91c1c',
                }}
              >
                {paymentMsg}
              </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => handlePayNow(paymentModalOrder)}
                disabled={paymentLoading}
                style={{ padding: '12px', fontSize: '0.95rem', fontWeight: 600 }}
              >
                {paymentLoading ? 'Đang gửi tín hiệu...' : 'Tôi đã hoàn tất thanh toán'}
              </button>

              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setPaymentModalOrder(null)}
                style={{ padding: '10px' }}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default OrderHistoryPage;
