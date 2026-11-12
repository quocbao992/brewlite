import React, { useEffect, useState } from 'react';
import { orderApi } from '../api';
import { formatService } from '../services/formatService';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { IconClock, IconCheck, IconClose } from '../components/common/Icons';

export const AdminOrdersPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadQueue();
  }, []);

  const loadQueue = async () => {
    try {
      setLoading(true);
      const res = await orderApi.getQueue();
      const list = Array.isArray(res) ? res : res?.data || res?.content || [];
      setOrders(list);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (orderId, newStatus) => {
    try {
      await orderApi.updateStatus(orderId, newStatus);
      loadQueue();
    } catch {
      setOrders((prev) =>
        prev.map((o) => (o.id === orderId ? { ...o, status: newStatus } : o))
      );
    }
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
        <div>
          <h1 style={{ fontSize: '2.2rem', fontWeight: 800 }}>
            Quản Lý Đơn Hàng & Quầy Pha Chế
          </h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>
            Theo dõi và cập nhật trạng thái đơn hàng thời gian thực
          </p>
        </div>

        <button className="btn btn-secondary btn-sm" onClick={loadQueue}>
          Tải lại danh sách
        </button>
      </div>

      {loading ? (
        <LoadingSpinner text="Đang tải hàng đợi..." />
      ) : orders.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <p style={{ fontSize: '1.2rem', color: 'var(--color-text-secondary)' }}>
            Hiện tại không có đơn hàng nào trong hàng đợi.
          </p>
        </div>
      ) : (
        <div className="product-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))' }}>
          {orders.map((order) => {
            const statusInfo = formatService.getOrderStatusInfo(order.status);
            return (
              <div
                key={order.id}
                style={{
                  background: 'var(--color-surface)',
                  padding: '20px',
                  borderRadius: 'var(--radius-lg)',
                  border: '1.5px solid var(--color-border)',
                  boxShadow: 'var(--shadow-sm)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '14px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 800, fontSize: '1.1rem' }}>
                    {order.orderCode || `#ORD-${order.id}`}
                  </span>
                  <span
                    className="status-badge"
                    style={{ background: statusInfo.bg, color: statusInfo.color }}
                  >
                    {statusInfo.label}
                  </span>
                </div>

                <div style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '6px' }}>
                    <span>
                      <strong>Khách hàng: </strong>
                      <span style={{ color: 'var(--color-text-main, #1e293b)', fontWeight: 600 }}>
                        {order.customerName || order.phone || 'Tài khoản thành viên'}
                      </span>
                      {order.phone && order.customerName && <span> ({order.phone})</span>}
                    </span>
                    <span style={{ padding: '2px 8px', borderRadius: '4px', background: '#e0f2fe', color: '#0369a1', fontSize: '0.78rem', fontWeight: 600 }}>
                      {formatService.formatPaymentMethod(order.paymentMethod)}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.82rem', color: 'var(--color-text-secondary)' }}>
                    <strong>{order.orderType === 'DINE_IN' ? `Tại bàn: ${order.tableNumber || 'Chưa chọn bàn'}` : 'Mang đi'}</strong>
                    {' • '}{formatService.formatDateTime(order.createdAt)}
                  </div>
                </div>

                <div
                  style={{
                    background: 'var(--color-surface-muted)',
                    padding: '12px',
                    borderRadius: 'var(--radius-md)',
                    fontSize: '0.9rem',
                  }}
                >
                  {Array.isArray(order.items) && order.items.length > 0 ? (
                    order.items.map((it, idx) => (
                      <div key={idx} style={{ padding: '3px 0', borderBottom: idx < order.items.length - 1 ? '1px dashed #e2e8f0' : 'none' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <span>
                            <strong>{it.quantity}x</strong> {it.productName || it.name} {it.size ? `(Size ${it.size})` : ''}
                          </span>
                          <span>{formatService.formatCurrency(it.lineTotal || (it.unitPrice * it.quantity) || 0)}</span>
                        </div>
                        {Array.isArray(it.toppings) && it.toppings.length > 0 && (
                          <div style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                            + Topping: {it.toppings.map((t) => t.toppingName || t.name || t).join(', ')}
                          </div>
                        )}
                      </div>
                    ))
                  ) : (
                    <div style={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.82rem' }}>
                      Không có thông tin món chi tiết
                    </div>
                  )}
                  <div style={{ borderTop: '1px solid var(--color-border)', marginTop: '8px', paddingTop: '6px', textAlign: 'right', fontWeight: 700, fontSize: '0.95rem' }}>
                    Tổng đơn: {formatService.formatCurrency(order.totalAmount || 0)}
                  </div>
                </div>

                <div style={{ marginTop: 'auto', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {order.status === 'PENDING' && (
                    <div style={{ display: 'flex', width: '100%', gap: '8px', alignItems: 'center' }}>
                      <span style={{ fontSize: '0.85rem', color: '#b45309', fontWeight: 600, flex: 1, display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                        <IconClock size={16} /> Chờ khách thanh toán...
                      </span>
                    </div>
                  )}

                  {order.status === 'PAID' && (
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={() => handleUpdateStatus(order.id, 'PREPARING')}
                      style={{ flex: 1, fontWeight: 700 }}
                    >
                      Bắt đầu pha chế
                    </button>
                  )}

                  {order.status === 'PREPARING' && (
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={() => handleUpdateStatus(order.id, 'READY')}
                      style={{ flex: 1, fontWeight: 700, background: '#d97706', borderColor: '#d97706' }}
                    >
                      Pha xong (Sẵn sàng giao)
                    </button>
                  )}

                  {order.status === 'READY' && (
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={() => handleUpdateStatus(order.id, 'COMPLETED')}
                      style={{ flex: 1, fontWeight: 700, background: '#16a34a', borderColor: '#16a34a' }}
                    >
                      Khách đã nhận (Hoàn tất)
                    </button>
                  )}

                  {order.status === 'COMPLETED' && (
                    <div style={{ width: '100%', textAlign: 'center', fontSize: '0.82rem', color: '#16a34a', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                      <IconCheck size={16} /> Đơn hàng đã hoàn tất
                    </div>
                  )}

                  {(order.status === 'CANCELLED' || order.status === 'PAYMENT_FAILED') && (
                    <div style={{ width: '100%', textAlign: 'center', fontSize: '0.82rem', color: '#dc2626', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                      <IconClose size={16} /> Đơn hàng đã bị hủy / Thanh toán thất bại
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default AdminOrdersPage;
