import React, { useEffect, useState } from 'react';
import { orderApi } from '../api';
import { formatService } from '../services/formatService';
import LoadingSpinner from '../components/common/LoadingSpinner';

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

                <div style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                  {order.orderType === 'DINE_IN'
                    ? `Tại bàn: ${order.tableNumber || 'Chưa chọn bàn'}`
                    : 'Mang đi'}
                  {' - '}
                  {formatService.formatDateTime(order.createdAt)}
                </div>

                <div
                  style={{
                    background: 'var(--color-surface-muted)',
                    padding: '12px',
                    borderRadius: 'var(--radius-md)',
                    fontSize: '0.9rem',
                  }}
                >
                  {order.items &&
                    order.items.map((it, idx) => (
                      <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', padding: '3px 0' }}>
                        <span>
                          <strong>{it.quantity}x</strong> {it.productName || it.name} ({it.size})
                        </span>
                      </div>
                    ))}
                </div>

                <div style={{ marginTop: 'auto', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <button
                    className="btn btn-sm btn-outline"
                    onClick={() => handleUpdateStatus(order.id, 'PREPARING')}
                  >
                    Đang pha chế
                  </button>
                  <button
                    className="btn btn-sm btn-primary"
                    onClick={() => handleUpdateStatus(order.id, 'READY')}
                  >
                    Sẵn sàng giao
                  </button>
                  <button
                    className="btn btn-sm btn-secondary"
                    onClick={() => handleUpdateStatus(order.id, 'COMPLETED')}
                  >
                    Hoàn tất
                  </button>
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
