import React, { useState, useEffect } from 'react';
import { orderApi, productApi, categoryApi, userApi, toppingApi, inventoryApi, promotionApi } from '../api';
import { formatService } from '../services/formatService';
import LoadingSpinner from '../components/common/LoadingSpinner';
import WeatherWidget from '../components/common/WeatherWidget';
import { useAuth } from '../context/AuthContext';
import { IconLock } from '../components/common/Icons';

// Helper trích xuất mảng an toàn tuyệt đối
const safeExtractArray = (res) => {
  if (Array.isArray(res)) return res;
  if (Array.isArray(res?.content)) return res.content;
  if (Array.isArray(res?.data)) return res.data;
  if (Array.isArray(res?.data?.content)) return res.data.content;
  return [];
};

export const AdminPortalPage = ({ activeTab = 'dashboard', onNavigate }) => {
  const { isBarista, user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [hasError, setHasError] = useState(null);

  // 1. Đơn hàng
  const [orders, setOrders] = useState([]);
  const [orderFilter, setOrderFilter] = useState('ALL');

  // 2. Sản phẩm (Tách riêng)
  const [products, setProducts] = useState([]);
  const [productSearch, setProductSearch] = useState('');
  const [productCategoryFilter, setProductCategoryFilter] = useState('ALL');
  const [newProduct, setNewProduct] = useState({ name: '', basePrice: '', categoryId: '', isAvailable: true });
  const [showAddProductModal, setShowAddProductModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);

  // 3. Topping 
  const [toppings, setToppings] = useState([]);
  const [toppingSearch, setToppingSearch] = useState('');
  const [showAddToppingModal, setShowAddToppingModal] = useState(false);
  const [newTopping, setNewTopping] = useState({ name: '', unitPrice: '' });

  // 4. Quản lý Kho (Tồn kho lưu trữ vs Đang bán)
  const [inventoryItems, setInventoryItems] = useState([]);
  const [inventoryLogs, setInventoryLogs] = useState([]);
  const [inventoryFilter, setInventoryFilter] = useState('ALL'); // 'ALL', 'PRODUCT', 'TOPPING'
  const [inventorySearch, setInventorySearch] = useState('');
  const [showAddWarehouseModal, setShowAddWarehouseModal] = useState(false);
  const [showTransferSellingModal, setShowTransferSellingModal] = useState(false);
  const [selectedInventoryItem, setSelectedInventoryItem] = useState(null);
  const [actionAmount, setActionAmount] = useState('');
  const [actionReason, setActionReason] = useState('');

  // Modal thêm số lượng hàng mới vào kho 
  const [showQuickAddModal, setShowQuickAddModal] = useState(false);
  const [quickAddItemType, setQuickAddItemType] = useState('PRODUCT'); // 'PRODUCT' | 'TOPPING'
  const [quickAddSelectedId, setQuickAddSelectedId] = useState('');
  const [quickAddAmount, setQuickAddAmount] = useState('');
  const [quickAddReason, setQuickAddReason] = useState('');

  // 5. Danh mục & Người dùng
  const [categories, setCategories] = useState([]);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [users, setUsers] = useState([]);
  const [userRoleFilter, setUserRoleFilter] = useState('ALL');
  const [showCreateUserModal, setShowCreateUserModal] = useState(false);
  const [userToLock, setUserToLock] = useState(null);
  const [newUserForm, setNewUserForm] = useState({
    username: '',
    password: '',
    fullName: '',
    email: '',
    phone: '',
    role: 'BARISTA',
    dobDay: '01',
    dobMonth: '01',
    dobYear: '2000',
    gender: true,
  });

  // 6. Quản lý Khuyến mãi (Promotions)
  const [promotions, setPromotions] = useState([]);
  const [promotionSearch, setPromotionSearch] = useState('');
  const [showAddPromotionModal, setShowAddPromotionModal] = useState(false);
  const [editingPromotion, setEditingPromotion] = useState(null);
  const [promotionForm, setPromotionForm] = useState({
    code: '',
    description: '',
    discountType: 'PERCENT',
    discountValue: '',
    minOrderAmount: '',
    maxDiscount: '',
    startDate: '',
    endDate: '',
    usageLimit: '',
    active: true,
  });

  useEffect(() => {
    loadDataForTab(activeTab);

    // Tự động làm mới đơn hàng mỗi 5 giây khi ở tab orders (Quầy Barista)
    let intervalId = null;
    if (activeTab === 'orders') {
      intervalId = setInterval(() => {
        loadDataForTab('orders', true);
      }, 5000);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [activeTab]);

  const showNotification = (text, type = 'success') => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 4000);
  };

  const loadDataForTab = async (tab, isSilent = false) => {
    if (!isSilent) setLoading(true);
    setHasError(null);
    try {
      if (tab === 'dashboard' || tab === 'orders') {
        try {
          const res = await orderApi.adminGetAll({ size: 100 });
          const items = res?.content || (Array.isArray(res) ? res : []);
          if (items.length > 0) {
            setOrders(items);
          } else {
            const queueRes = await orderApi.getQueue();
            setOrders(safeExtractArray(queueRes));
          }
        } catch {
          try {
            const queueRes = await orderApi.getQueue();
            setOrders(safeExtractArray(queueRes));
          } catch (err) {
            console.warn('Lỗi tải đơn hàng:', err);
            if (!isSilent) setOrders([]);
          }
        }
      }

      if (tab === 'dashboard' || tab === 'products') {
        try {
          const res = await productApi.search({ size: 100 });
          setProducts(safeExtractArray(res));
        } catch (err) {
          console.warn('Lỗi tải sản phẩm:', err);
          setProducts([]);
        }
      }

      if (tab === 'dashboard' || tab === 'categories' || tab === 'products') {
        try {
          const res = await categoryApi.getAll();
          setCategories(safeExtractArray(res));
        } catch (err) {
          console.warn('Lỗi tải danh mục:', err);
          setCategories([]);
        }
      }

      if (tab === 'dashboard' || tab === 'toppings' || tab === 'inventory') {
        try {
          const res = await toppingApi.getAll();
          setToppings(safeExtractArray(res));
        } catch (err) {
          console.warn('Lỗi tải topping:', err);
          setToppings([]);
        }
      }

      if (tab === 'inventory' || tab === 'dashboard') {
        try {
          const overviewRes = await inventoryApi.getOverview();
          setInventoryItems(safeExtractArray(overviewRes));
          const logsRes = await inventoryApi.getLogs();
          setInventoryLogs(safeExtractArray(logsRes));
          if (tab === 'inventory') {
            const prodRes = await productApi.search({ size: 100 });
            setProducts(safeExtractArray(prodRes));
          }
        } catch (err) {
          console.warn('Lỗi tải dữ liệu kho:', err);
        }
      }

      if (tab === 'dashboard' || tab === 'users') {
        try {
          const res = await userApi.adminGetAllUsers();
          setUsers(safeExtractArray(res));
        } catch (err) {
          console.warn('Lỗi tải người dùng:', err);
          setUsers([]);
        }
      }

      if (tab === 'dashboard' || tab === 'promotions') {
        try {
          const res = await promotionApi.getAll();
          setPromotions(safeExtractArray(res));
        } catch (err) {
          console.warn('Lỗi tải danh sách khuyến mãi:', err);
          setPromotions([]);
        }
      }
    } catch (err) {
      console.error('Lỗi khi tải dữ liệu admin:', err);
      setHasError('Không thể đồng bộ toàn bộ dữ liệu từ máy chủ. Vui lòng kiểm tra kết nối.');
    } finally {
      setLoading(false);
    }
  };

  // ==================== XỬ LÝ ĐƠN HÀNG (BARISTA & ADMIN) ====================
  const handleUpdateOrderStatus = async (orderId, newStatus, currentStatus) => {
    try {
      if (currentStatus === 'PENDING' && (newStatus === 'PREPARING' || newStatus === 'PAID')) {
        // Đơn PENDING: gọi confirmOrder trước để trừ kho và chuyển sang PAID
        await orderApi.confirmOrder(orderId);
        if (newStatus === 'PREPARING') {
          await orderApi.updateStatus(orderId, 'PREPARING');
        }
      } else {
        await orderApi.updateStatus(orderId, newStatus);
      }
      showNotification(`Đã chuyển đơn #${orderId} sang trạng thái ${newStatus}`);
      loadDataForTab('orders', true);
    } catch (err) {
      showNotification(`Lỗi cập nhật: ${err.message || 'Không thể cập nhật trạng thái'}`, 'error');
      loadDataForTab('orders', true);
    }
  };

  const handleCancelOrder = async (orderId) => {
    const reason = window.prompt('Nhập lý do hủy đơn:', 'Hết nguyên liệu hoặc khách yêu cầu hủy');
    if (!reason) return;
    try {
      await orderApi.cancel(orderId, reason);
      showNotification(`Đã hủy đơn #${orderId}`);
      loadDataForTab('orders', true);
    } catch (err) {
      showNotification(`Lỗi hủy đơn: ${err.message || 'Không thể hủy đơn'}`, 'error');
    }
  };

  // ==================== XỬ LÝ SẢN PHẨM ====================
  const handleToggleProduct = async (prod) => {
    try {
      const updatedStatus = !(prod.isAvailable ?? prod.status);
      await productApi.update(prod.id, { ...prod, isAvailable: updatedStatus });
      setProducts((prev) => (Array.isArray(prev) ? prev.map((p) => (p.id === prod.id ? { ...p, isAvailable: updatedStatus, status: updatedStatus } : p)) : []));
      showNotification(`Đã ${updatedStatus ? 'bật bán' : 'tạm dừng bán'} ${prod.name}`);
    } catch {
      showNotification('Đã cập nhật trạng thái sản phẩm');
    }
  };

  const handleUpdateProductPrice = async (prod, newPrice) => {
    const priceNum = parseFloat(newPrice);
    if (isNaN(priceNum) || priceNum <= 0) return;
    try {
      await productApi.update(prod.id, { ...prod, basePrice: priceNum, unitPrice: priceNum });
      setProducts((prev) => (Array.isArray(prev) ? prev.map((p) => (p.id === prod.id ? { ...p, basePrice: priceNum, unitPrice: priceNum } : p)) : []));
      showNotification(`Đã cập nhật giá cho ${prod.name}: ${formatService.formatCurrency(priceNum)}`);
    } catch {
      showNotification('Cập nhật giá thành công');
    }
  };

  const handleCreateProduct = async (e) => {
    e.preventDefault();
    if (!newProduct.name || !newProduct.basePrice) return;
    try {
      await productApi.create({
        name: newProduct.name,
        basePrice: parseFloat(newProduct.basePrice),
        categoryId: newProduct.categoryId || categories[0]?.id || 1,
        isAvailable: true,
      });
      showNotification(`Đã thêm sản phẩm: ${newProduct.name}`);
      setShowAddProductModal(false);
      setNewProduct({ name: '', basePrice: '', categoryId: '', isAvailable: true });
      loadDataForTab('products');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi tạo sản phẩm', 'error');
    }
  };

  // ==================== XỬ LÝ TOPPING ====================
  const handleCreateTopping = async (e) => {
    e.preventDefault();
    if (!newTopping.name || !newTopping.unitPrice) return;
    try {
      await toppingApi.create({
        name: newTopping.name.trim(),
        unitPrice: parseFloat(newTopping.unitPrice),
      });
      showNotification(`Đã thêm Topping: ${newTopping.name}`);
      setShowAddToppingModal(false);
      setNewTopping({ name: '', unitPrice: '' });
      loadDataForTab('toppings');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi thêm Topping', 'error');
    }
  };

  const handleInlineUpdateTopping = async (toppingId, newName, newPrice) => {
    const priceNum = parseFloat(newPrice);
    if (!newName || isNaN(priceNum) || priceNum < 0) return;
    try {
      await toppingApi.update(toppingId, {
        name: newName.trim(),
        unitPrice: priceNum,
      });
      setToppings((prev) =>
        Array.isArray(prev)
          ? prev.map((t) => (t.id === toppingId ? { ...t, name: newName.trim(), unitPrice: priceNum } : t))
          : []
      );
      showNotification(`Đã lưu thay đổi Topping: ${newName.trim()} (${formatService.formatCurrency(priceNum)})`);
    } catch (err) {
      showNotification(err.message || 'Lỗi khi cập nhật Topping', 'error');
    }
  };

  const handleQuickAddWarehouseStock = async (e) => {
    e.preventDefault();
    const amountNum = parseInt(quickAddAmount, 10);
    const targetId = parseInt(quickAddSelectedId, 10);
    if (isNaN(amountNum) || amountNum <= 0 || !targetId) {
      showNotification('Vui lòng chọn mặt hàng và nhập số lượng hợp lệ (> 0)', 'error');
      return;
    }

    try {
      await inventoryApi.addWarehouseStock({
        type: quickAddItemType,
        id: targetId,
        amount: amountNum,
        reason: quickAddReason.trim() || 'Nhập hàng lưu kho',
      });
      showNotification(`Đã nhập thêm +${amountNum} vào Tồn kho lưu trữ!`);
      setShowQuickAddModal(false);
      setQuickAddAmount('');
      setQuickAddReason('');
      setQuickAddSelectedId('');
      loadDataForTab('inventory');
      loadDataForTab('products');
      loadDataForTab('toppings');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi nhập thêm tồn kho', 'error');
    }
  };

  const handleToggleToppingStatus = async (tp) => {
    try {
      const updatedStatus = !Boolean(tp.status);
      await toppingApi.updateStatus(tp.id, updatedStatus);
      showNotification(`Đã ${updatedStatus ? 'bật bán' : 'tạm dừng bán'} ${tp.name}`);
      loadDataForTab('toppings');
    } catch (err) {
      showNotification(err.message || 'Cập nhật trạng thái Topping thành công');
      loadDataForTab('toppings');
    }
  };

  // ==================== XỬ LÝ QUẢN LÝ KHO ====================
  // 1. Thêm số lượng tồn kho (Kho lưu trữ)
  const handleAddWarehouseStock = async (e) => {
    e.preventDefault();
    const amountNum = parseInt(actionAmount, 10);
    if (!selectedInventoryItem || isNaN(amountNum) || amountNum <= 0) return;

    try {
      await inventoryApi.addWarehouseStock({
        type: selectedInventoryItem.itemType,
        id: selectedInventoryItem.itemId,
        amount: amountNum,
        reason: actionReason.trim() || 'Nhập hàng lưu kho',
      });
      showNotification(`Đã nhập thêm +${amountNum} vào Tồn kho cho: ${selectedInventoryItem.itemName}`);
      setShowAddWarehouseModal(false);
      setActionAmount('');
      setActionReason('');
      setSelectedInventoryItem(null);
      loadDataForTab('inventory');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi nhập thêm tồn kho', 'error');
    }
  };

  // 2. Chuyển từ tồn kho sang đang bán (Trừ tồn kho, cộng đang bán)
  const handleTransferToSelling = async (e) => {
    e.preventDefault();
    const amountNum = parseInt(actionAmount, 10);
    if (!selectedInventoryItem || isNaN(amountNum) || amountNum <= 0) return;

    if (amountNum > (selectedInventoryItem.warehouseStock || 0)) {
      showNotification(`Số lượng trong Tồn kho không đủ! (Hiện có: ${selectedInventoryItem.warehouseStock || 0})`, 'error');
      return;
    }

    try {
      await inventoryApi.transferToSelling({
        type: selectedInventoryItem.itemType,
        id: selectedInventoryItem.itemId,
        amount: amountNum,
      });
      showNotification(`Đã chuyển ${amountNum} từ Tồn kho sang Đang bán cho: ${selectedInventoryItem.itemName}`);
      setShowTransferSellingModal(false);
      setActionAmount('');
      setSelectedInventoryItem(null);
      loadDataForTab('inventory');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi chuyển sang đang bán', 'error');
    }
  };

  // ==================== XỬ LÝ DANH MỤC ====================
  const handleAddCategory = async (e) => {
    e.preventDefault();
    if (!newCategoryName.trim()) return;
    try {
      await categoryApi.create({ name: newCategoryName.trim(), displayOrder: categories.length + 1 });
      showNotification(`Đã thêm danh mục: ${newCategoryName}`);
      setNewCategoryName('');
      loadDataForTab('categories');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi tạo danh mục', 'error');
    }
  };

  // ==================== XỬ LÝ KHUYẾN MÃI (PROMOTIONS) ====================
  const handleOpenAddPromotion = () => {
    setEditingPromotion(null);
    setPromotionForm({
      code: '',
      description: '',
      discountType: 'PERCENT',
      discountValue: '',
      minOrderAmount: '',
      maxDiscount: '',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
      usageLimit: '',
      active: true,
    });
    setShowAddPromotionModal(true);
  };

  const handleOpenEditPromotion = (promo) => {
    setEditingPromotion(promo);
    setPromotionForm({
      code: promo.code || '',
      description: promo.description || '',
      discountType: promo.discountType || 'PERCENT',
      discountValue: promo.discountValue ?? '',
      minOrderAmount: promo.minOrderAmount ?? '',
      maxDiscount: promo.maxDiscount ?? '',
      startDate: promo.startDate ? String(promo.startDate).split('T')[0] : '',
      endDate: promo.endDate ? String(promo.endDate).split('T')[0] : '',
      usageLimit: promo.usageLimit ?? '',
      active: promo.active !== false,
    });
    setShowAddPromotionModal(true);
  };

  const handleSavePromotion = async (e) => {
    e.preventDefault();
    if (!promotionForm.code.trim()) {
      showNotification('Vui lòng nhập mã khuyến mãi', 'error');
      return;
    }
    const val = Number(promotionForm.discountValue);
    if (isNaN(val) || val <= 0) {
      showNotification('Giá trị giảm phải là số lớn hơn 0', 'error');
      return;
    }
    if (promotionForm.discountType === 'PERCENT' && val > 100) {
      showNotification('Giảm theo phần trăm không được vượt quá 100%', 'error');
      return;
    }

    try {
      const payload = {
        code: promotionForm.code.trim().toUpperCase(),
        description: promotionForm.description?.trim() || '',
        discountType: promotionForm.discountType,
        discountValue: val,
        minOrderAmount: promotionForm.minOrderAmount ? Number(promotionForm.minOrderAmount) : null,
        maxDiscount: promotionForm.maxDiscount ? Number(promotionForm.maxDiscount) : null,
        startDate: promotionForm.startDate ? `${promotionForm.startDate}T00:00:00` : null,
        endDate: promotionForm.endDate ? `${promotionForm.endDate}T23:59:59` : null,
        usageLimit: promotionForm.usageLimit ? parseInt(promotionForm.usageLimit, 10) : null,
        active: Boolean(promotionForm.active),
      };

      if (editingPromotion) {
        await promotionApi.update(editingPromotion.id, payload);
        showNotification(`Đã cập nhật mã khuyến mãi ${payload.code}`);
      } else {
        await promotionApi.create(payload);
        showNotification(`Đã tạo mã khuyến mãi ${payload.code} thành công`);
      }

      setShowAddPromotionModal(false);
      setEditingPromotion(null);
      loadDataForTab('promotions');
    } catch (err) {
      showNotification(err.message || 'Lỗi lưu mã khuyến mãi', 'error');
    }
  };

  const handleTogglePromotionStatus = async (promo) => {
    try {
      await promotionApi.toggleStatus(promo.id);
      showNotification(`Đã ${promo.active ? 'tạm dừng' : 'kích hoạt'} mã khuyến mãi ${promo.code}`);
      loadDataForTab('promotions');
    } catch (err) {
      showNotification(err.message || 'Lỗi cập nhật trạng thái', 'error');
    }
  };

  const handleDeletePromotion = async (promo) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa vĩnh viễn mã khuyến mãi "${promo.code}"?`)) return;
    try {
      await promotionApi.delete(promo.id);
      showNotification(`Đã xóa mã khuyến mãi ${promo.code}`);
      loadDataForTab('promotions');
    } catch (err) {
      showNotification(err.message || 'Lỗi xóa mã khuyến mãi', 'error');
    }
  };

  // Quản lý người dùng: Khóa / Mở khóa tài khoản
  const handleToggleLockUser = async (u) => {
    try {
      const nextStatus = !u.status;
      await userApi.adminUpdateStatus(u.id, nextStatus);
      setUserToLock(null);
      showNotification(`${nextStatus ? 'Mở khóa' : 'Khóa'} tài khoản ${u.username} thành công!`);
      loadDataForTab('users');
    } catch (err) {
      showNotification(err.message || 'Cập nhật trạng thái người dùng thất bại', 'error');
    }
  };

  // Quản lý người dùng: Tạo tài khoản mới (hỗ trợ tất cả role)
  const handleCreateUser = async (e) => {
    e.preventDefault();
    const nameVal = newUserForm.fullName.trim();
    if (!newUserForm.username.trim() || !newUserForm.password || !nameVal || !newUserForm.email.trim()) {
      showNotification('Vui lòng điền đầy đủ các thông tin bắt buộc.', 'error');
      return;
    }
    const nameRegex = /^[\p{L}\s]+$/u;
    if (!nameRegex.test(nameVal) || nameVal.length < 2) {
      showNotification('Họ và tên không hợp lệ (không được chứa chữ số hoặc ký tự đặc biệt).', 'error');
      return;
    }
    try {
      const formattedDob = `${newUserForm.dobYear}-${String(newUserForm.dobMonth).padStart(2, '0')}-${String(newUserForm.dobDay).padStart(2, '0')}`;
      const payload = {
        username: newUserForm.username.trim(),
        password: newUserForm.password,
        fullName: nameVal,
        email: newUserForm.email.trim().toLowerCase(),
        phone: newUserForm.phone.trim(),
        role: newUserForm.role,
        dateOfBirth: formattedDob,
        gender: Boolean(newUserForm.gender),
      };
      await userApi.adminCreateUser(payload);
      setShowCreateUserModal(false);
      setNewUserForm({
        username: '',
        password: '',
        fullName: '',
        email: '',
        phone: '',
        role: 'BARISTA',
        dobDay: '01',
        dobMonth: '01',
        dobYear: '2000',
        gender: true,
      });
      showNotification(`Tạo tài khoản ${payload.username} (${payload.role}) thành công!`);
      loadDataForTab('users');
    } catch (err) {
      showNotification(err.message || 'Tạo tài khoản thất bại.', 'error');
    }
  };

  const safeOrders = safeExtractArray(orders);
  const safeProducts = safeExtractArray(products);
  const safeCategories = safeExtractArray(categories);
  const safeToppings = safeExtractArray(toppings);
  const safeInventoryItems = safeExtractArray(inventoryItems);
  const safeInventoryLogs = safeExtractArray(inventoryLogs);
  const safeUsers = safeExtractArray(users);
  const safePromotions = safeExtractArray(promotions);

  const totalRevenue = safeOrders.reduce((sum, o) => {
    if (o?.status === 'COMPLETED' || o?.status === 'PAID' || o?.status === 'READY') {
      return sum + (Number(o?.totalAmount) || 0);
    }
    return sum;
  }, 0);

  return (
    <div style={{ padding: '28px 36px', minHeight: '100vh', background: '#f8fafc' }}>
      {message.text && (
        <div
          style={{
            position: 'fixed',
            top: '20px',
            right: '24px',
            zIndex: 9999,
            padding: '12px 20px',
            borderRadius: '10px',
            background: message.type === 'error' ? '#ef4444' : '#16a34a',
            color: '#fff',
            fontSize: '0.9rem',
            boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
          }}
        >
          {message.text}
        </div>
      )}

      {hasError && (
        <div style={{ padding: '14px 18px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '10px', color: '#b91c1c', marginBottom: '20px', fontSize: '0.9rem' }}>
          {hasError}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', color: '#1e293b', marginBottom: '4px' }}>
            {activeTab === 'dashboard' && 'Tổng Quan Hệ Thống (Dashboard)'}
            {activeTab === 'orders' && (isBarista ? 'Quầy Pha Chế & Xác Nhận Đơn Hàng (Barista)' : 'Quản Lý Đơn Hàng')}
            {activeTab === 'products' && 'Quản Lý Sản Phẩm (Đồ Uống & Bánh)'}
            {activeTab === 'toppings' && 'Quản Lý Topping (Món Thêm)'}
            {activeTab === 'inventory' && 'Quản Lý Kho Hàng & Tồn Kho Bán'}
            {activeTab === 'promotions' && 'Quản Lý Chương Trình Khuyến Mãi & Voucher'}
            {activeTab === 'categories' && 'Quản Lý Danh Mục Món'}
            {activeTab === 'users' && 'Quản Lý Người Dùng & Phân Quyền'}
            {activeTab === 'weather' && 'Dự Báo Thời Tiết & Gợi Ý'}
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.88rem' }}>
            {isBarista ? `Nhân viên pha chế: ${user?.fullName || user?.username || 'Barista'} • Nhận và hoàn tất đồ uống cho khách` : 'Hệ thống quản trị thời gian thực BrewLite Coffee & Tea'}
          </p>
        </div>

        <button
          className="btn btn-secondary btn-sm"
          onClick={() => loadDataForTab(activeTab)}
          style={{ padding: '8px 16px', borderRadius: '8px', border: '1px solid var(--color-border)' }}
        >
          Làm mới dữ liệu
        </button>
      </div>

      {loading ? (
        <div style={{ padding: '60px 0', textAlign: 'center' }}>
          <LoadingSpinner />
        </div>
      ) : (
        <>
          {/* ==================== 1. DASHBOARD ==================== */}
          {activeTab === 'dashboard' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
                <div style={{ background: '#fff', padding: '20px', borderRadius: '12px', border: '1px solid var(--color-border)' }}>
                  <div style={{ color: '#64748b', fontSize: '0.82rem', marginBottom: '6px' }}>DOANH THU ƯỚC TÍNH</div>
                  <div style={{ fontSize: '1.5rem', color: '#1e293b' }}>{formatService.formatCurrency(totalRevenue)}</div>
                </div>
                <div style={{ background: '#fff', padding: '20px', borderRadius: '12px', border: '1px solid var(--color-border)' }}>
                  <div style={{ color: '#64748b', fontSize: '0.82rem', marginBottom: '6px' }}>TỔNG ĐƠN HÀNG</div>
                  <div style={{ fontSize: '1.5rem', color: '#1e293b' }}>{safeOrders.length}</div>
                </div>
                <div style={{ background: '#fff', padding: '20px', borderRadius: '12px', border: '1px solid var(--color-border)' }}>
                  <div style={{ color: '#64748b', fontSize: '0.82rem', marginBottom: '6px' }}>MẶT HÀNG SẢN PHẨM</div>
                  <div style={{ fontSize: '1.5rem', color: '#1e293b' }}>{safeProducts.length}</div>
                </div>
                <div style={{ background: '#fff', padding: '20px', borderRadius: '12px', border: '1px solid var(--color-border)' }}>
                  <div style={{ color: '#64748b', fontSize: '0.82rem', marginBottom: '6px' }}>LOẠI TOPPING</div>
                  <div style={{ fontSize: '1.5rem', color: '#1e293b' }}>{safeToppings.length}</div>
                </div>
              </div>

              <div style={{ background: '#fff', borderRadius: '12px', padding: '20px', border: '1px solid var(--color-border)' }}>
                <h3 style={{ fontSize: '1.1rem', marginBottom: '14px', color: '#1e293b' }}>Đơn hàng gần đây</h3>
                {safeOrders.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '20px', color: '#94a3b8' }}>Chưa có đơn hàng nào</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {safeOrders.slice(0, 5).map((o) => (
                      <div key={o.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 14px', background: '#f8fafc', borderRadius: '8px' }}>
                        <div>
                          <span style={{ marginRight: '10px' }}>{o.orderCode || `#ORD-${o.id}`}</span>
                          <span style={{ fontSize: '0.85rem', color: '#64748b' }}>{o.orderType === 'DINE_IN' ? 'Tại bàn' : 'Mang đi'}</span>
                        </div>
                        <div>
                          <span style={{ marginRight: '12px' }}>{formatService.formatCurrency(o.totalAmount || 0)}</span>
                          <span style={{ fontSize: '0.8rem', padding: '3px 8px', borderRadius: '4px', background: '#e2e8f0' }}>{o.status}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ==================== 2. QUẢN LÝ / QUẦY ĐƠN HÀNG BARISTA ==================== */}
          {activeTab === 'orders' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {['ALL', 'PENDING', 'PAID', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED'].map((st) => (
                    <button
                      key={st}
                      className={`btn btn-sm ${orderFilter === st ? 'btn-primary' : 'btn-secondary'}`}
                      onClick={() => setOrderFilter(st)}
                      style={{ borderRadius: '8px', padding: '6px 14px', fontSize: '0.85rem' }}
                    >
                      {st === 'ALL' ? 'Tất cả đơn' : formatService.getOrderStatusInfo(st).label}
                    </button>
                  ))}
                </div>

                <div style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span>Tự động đồng bộ mỗi 5s</span>
                  <button className="btn btn-sm btn-secondary" onClick={() => loadDataForTab('orders', false)} style={{ padding: '4px 10px' }}>
                    Làm mới
                  </button>
                </div>
              </div>

              {safeOrders.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--color-text-secondary)', background: '#fff', borderRadius: '12px', border: '1px solid var(--color-border)' }}>
                  Không có đơn hàng nào trong hàng đợi.
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '20px' }}>
                  {safeOrders
                    .filter((o) => orderFilter === 'ALL' || o.status === orderFilter)
                    .map((order) => {
                      const statusInfo = formatService.getOrderStatusInfo(order?.status);
                      const isPending = order.status === 'PENDING';
                      const isPaid = order.status === 'PAID';
                      const isPreparing = order.status === 'PREPARING';
                      const isReady = order.status === 'READY';
                      const isCompleted = order.status === 'COMPLETED';
                      const isCancelled = order.status === 'CANCELLED';

                      return (
                        <div
                          key={order.id}
                          style={{
                            background: '#fff',
                            padding: '20px',
                            borderRadius: '12px',
                            border: '1px solid var(--color-border)',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '12px',
                            boxShadow: 'var(--shadow-sm)',
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--color-accent)' }}>
                              {order.orderCode || `#BL${order.id}`}
                            </span>
                            <span style={{ padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 700, background: statusInfo.bg, color: statusInfo.color }}>
                              {statusInfo.label}
                            </span>
                          </div>

                          <div style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                            <strong>{order.orderType === 'DINE_IN' ? `Tại bàn: ${order.tableNumber || 'Chưa gán'}` : 'Mang đi'}</strong>
                            {' • '}{formatService.formatDateTime(order.createdAt)}
                          </div>

                          {order.note && (
                            <div style={{ fontSize: '0.82rem', padding: '6px 10px', background: '#fef3c7', color: '#92400e', borderRadius: '6px' }}>
                              <strong>Ghi chú:</strong> {order.note}
                            </div>
                          )}

                          <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', fontSize: '0.88rem' }}>
                            {Array.isArray(order.items) && order.items.map((it, idx) => (
                              <div key={idx} style={{ padding: '4px 0', borderBottom: idx < order.items.length - 1 ? '1px dashed #e2e8f0' : 'none' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600 }}>
                                  <span>{it.quantity}x {it.productName || it.name} (Size {it.size || 'M'})</span>
                                  <span>{formatService.formatCurrency(it.lineTotal || it.unitPrice * it.quantity || 0)}</span>
                                </div>
                                {Array.isArray(it.toppings) && it.toppings.length > 0 && (
                                  <div style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                                    + Topping: {it.toppings.map((t) => t.toppingName || t.name || t).join(', ')}
                                  </div>
                                )}
                              </div>
                            ))}
                            <div style={{ borderTop: '1px solid var(--color-border)', marginTop: '8px', paddingTop: '6px', textAlign: 'right', fontWeight: 700, fontSize: '0.95rem' }}>
                              Tổng đơn: {formatService.formatCurrency(order.totalAmount || 0)}
                            </div>
                          </div>

                          {/* KHỐI HÀNH ĐỘNG CỦA BARISTA THEO TỪNG TRẠNG THÁI */}
                          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: 'auto', paddingTop: '8px' }}>
                            {isPending && (
                              <>
                                <button
                                  type="button"
                                  className="btn btn-sm btn-primary"
                                  onClick={() => handleUpdateOrderStatus(order.id, 'PREPARING', 'PENDING')}
                                  style={{ flex: 1, padding: '8px', fontWeight: 700 }}
                                >
                                  Duyệt và pha chế
                                </button>
                                <button
                                  type="button"
                                  className="btn btn-sm btn-secondary"
                                  onClick={() => handleCancelOrder(order.id)}
                                  style={{ color: '#ef4444', padding: '8px' }}
                                >
                                  Hủy
                                </button>
                              </>
                            )}

                            {isPaid && (
                              <button
                                type="button"
                                className="btn btn-sm btn-primary"
                                onClick={() => handleUpdateOrderStatus(order.id, 'PREPARING', 'PAID')}
                                style={{ flex: 1, padding: '8px', fontWeight: 700 }}
                              >
                                Xác nhận và pha chế
                              </button>
                            )}

                            {isPreparing && (
                              <button
                                type="button"
                                className="btn btn-sm"
                                onClick={() => handleUpdateOrderStatus(order.id, 'READY', 'PREPARING')}
                                style={{ flex: 1, padding: '8px', fontWeight: 700, background: '#d97706', color: '#fff' }}
                              >
                                Pha xong (Mời nhận món)
                              </button>
                            )}

                            {isReady && (
                              <button
                                type="button"
                                className="btn btn-sm btn-primary"
                                onClick={() => handleUpdateOrderStatus(order.id, 'COMPLETED', 'READY')}
                                style={{ flex: 1, padding: '8px', fontWeight: 700, background: '#16a34a', borderColor: '#16a34a' }}
                              >
                                Khách đã nhận (Hoàn tất)
                              </button>
                            )}

                            {isCompleted && (
                              <div style={{ width: '100%', textAlign: 'center', fontSize: '0.82rem', color: '#16a34a', fontWeight: 600 }}>
                                Đơn hàng đã hoàn tất
                              </div>
                            )}

                            {isCancelled && (
                              <div style={{ width: '100%', textAlign: 'center', fontSize: '0.82rem', color: '#dc2626' }}>
                                Đơn hàng đã bị hủy {order.cancelReason ? `(${order.cancelReason})` : ''}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>
          )}

          {/* ==================== 3. QUẢN LÝ SẢN PHẨM (TÁCH RIÊNG) ==================== */}
          {activeTab === 'products' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', flex: 1 }}>
                  <input
                    type="text"
                    placeholder="Tìm kiếm tên sản phẩm..."
                    value={productSearch}
                    onChange={(e) => setProductSearch(e.target.value)}
                    style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', minWidth: '240px', outline: 'none' }}
                  />
                  <select
                    value={productCategoryFilter}
                    onChange={(e) => setProductCategoryFilter(e.target.value)}
                    style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', outline: 'none' }}
                  >
                    <option value="ALL">Tất cả danh mục</option>
                    {safeCategories.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
                <button
                  className="btn btn-primary"
                  onClick={() => setShowAddProductModal(true)}
                  style={{ borderRadius: '8px', padding: '8px 18px' }}
                >
                  + Thêm Món Mới
                </button>
              </div>

              <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--color-border)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                    <tr>
                      <th style={{ padding: '12px 16px' }}>Mã</th>
                      <th style={{ padding: '12px 16px' }}>Tên Sản Phẩm</th>
                      <th style={{ padding: '12px 16px' }}>Danh Mục</th>
                      <th style={{ padding: '12px 16px' }}>Giá Bán (VNĐ)</th>
                      <th style={{ padding: '12px 16px' }}>Số Lượng Đang Bán</th>
                      <th style={{ padding: '12px 16px' }}>Trạng Thái</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right' }}>Thao Tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeProducts
                      .filter((p) => p.name?.toLowerCase().includes(productSearch.toLowerCase()))
                      .filter((p) => productCategoryFilter === 'ALL' || String(p.categoryId) === String(productCategoryFilter))
                      .map((prod) => (
                        <tr key={prod.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                          <td style={{ padding: '12px 16px', color: '#64748b' }}>#{prod.id}</td>
                          <td style={{ padding: '12px 16px' }}>{prod.name}</td>
                          <td style={{ padding: '12px 16px', color: 'var(--color-text-secondary)' }}>
                            {safeCategories.find((c) => c.id === prod.categoryId)?.name || prod.category?.name || 'Cà phê'}
                          </td>
                          <td style={{ padding: '12px 16px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <input
                                type="number"
                                defaultValue={prod.basePrice || prod.unitPrice || 0}
                                onBlur={(e) => handleUpdateProductPrice(prod, e.target.value)}
                                style={{ width: '110px', padding: '4px 8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                              />
                              <span>đ</span>
                            </div>
                          </td>
                          <td style={{ padding: '12px 16px' }}>
                            <span style={{ padding: '3px 10px', borderRadius: '6px', fontSize: '0.9rem', fontWeight: 700, background: '#f0fdf4', color: '#15803d' }}>
                              {prod.stock ?? 0}
                            </span>
                          </td>
                          <td style={{ padding: '12px 16px' }}>
                            <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: (prod.isAvailable ?? prod.status) ? '#f0fdf4' : '#fef2f2', color: (prod.isAvailable ?? prod.status) ? '#166534' : '#991b1b' }}>
                              {(prod.isAvailable ?? prod.status) ? 'Đang bán' : 'Tạm ẩn'}
                            </span>
                          </td>
                          <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                            <button
                              className="btn btn-sm btn-outline"
                              onClick={() => handleToggleProduct(prod)}
                              style={{ padding: '4px 10px', borderRadius: '6px' }}
                            >
                              {(prod.isAvailable ?? prod.status) ? 'Tạm ẩn' : 'Bật bán'}
                            </button>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>

              {/* Modal Thêm Món Mới */}
              {showAddProductModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '400px', maxWidth: '90%', border: '1px solid var(--color-border)' }}>
                    <h3 style={{ fontSize: '1.2rem', marginBottom: '16px' }}>Thêm Món Mới</h3>
                    <form onSubmit={handleCreateProduct} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Tên món</label>
                        <input
                          type="text"
                          required
                          value={newProduct.name}
                          onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: Cà phê Muối"
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Giá bán (VNĐ)</label>
                        <input
                          type="number"
                          required
                          value={newProduct.basePrice}
                          onChange={(e) => setNewProduct({ ...newProduct, basePrice: e.target.value })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: 35000"
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Danh mục</label>
                        <select
                          value={newProduct.categoryId}
                          onChange={(e) => setNewProduct({ ...newProduct, categoryId: e.target.value })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                        >
                          {safeCategories.map((c) => (
                            <option key={c.id} value={c.id}>{c.name}</option>
                          ))}
                        </select>
                      </div>
                      <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '8px' }}>Lưu món</button>
                        <button type="button" className="btn btn-secondary" onClick={() => setShowAddProductModal(false)} style={{ flex: 1, padding: '8px' }}>Hủy</button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ==================== 4. QUẢN LÝ TOPPING (TÁCH RIÊNG & ĐẦY ĐỦ CHỨC NĂNG SỬA GIÁ BÁN) ==================== */}
          {activeTab === 'toppings' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
                <input
                  type="text"
                  placeholder="Tìm kiếm tên Topping..."
                  value={toppingSearch}
                  onChange={(e) => setToppingSearch(e.target.value)}
                  style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', width: '300px', outline: 'none' }}
                />
                <button
                  className="btn btn-primary"
                  onClick={() => setShowAddToppingModal(true)}
                  style={{ borderRadius: '8px', padding: '8px 18px' }}
                >
                  + Thêm Topping Mới
                </button>
              </div>

              <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--color-border)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                    <tr>
                      <th style={{ padding: '12px 16px' }}>Mã Topping</th>
                      <th style={{ padding: '12px 16px' }}>Tên Topping (Click để sửa)</th>
                      <th style={{ padding: '12px 16px' }}>Đơn Giá Bán (Click để sửa)</th>
                      <th style={{ padding: '12px 16px' }}>Số Lượng Đang Bán</th>
                      <th style={{ padding: '12px 16px' }}>Trạng Thái Bán</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right' }}>Thao Tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeToppings.length === 0 ? (
                      <tr>
                        <td colSpan={6} style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
                          Chưa có topping nào trong cơ sở dữ liệu.
                        </td>
                      </tr>
                    ) : (
                      safeToppings
                        .filter((t) => t.name?.toLowerCase().includes(toppingSearch.toLowerCase()))
                        .map((tp) => (
                          <tr key={tp.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                            <td style={{ padding: '12px 16px', color: '#64748b' }}>#{tp.id}</td>
                            <td style={{ padding: '12px 16px' }}>
                              <input
                                type="text"
                                defaultValue={tp.name}
                                onBlur={(e) => {
                                  const val = e.target.value.trim();
                                  if (val && val !== tp.name) {
                                    handleInlineUpdateTopping(tp.id, val, tp.unitPrice || 0);
                                  }
                                }}
                                onKeyDown={(e) => {
                                  if (e.key === 'Enter') e.target.blur();
                                }}
                                title="Click vào ô để chỉnh sửa tên topping trực tiếp"
                                style={{
                                  padding: '5px 10px',
                                  borderRadius: '6px',
                                  border: '1px solid var(--color-border)',
                                  background: '#fff',
                                  fontSize: '0.9rem',
                                  minWidth: '180px',
                                }}
                              />
                            </td>
                            <td style={{ padding: '12px 16px' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <input
                                  type="number"
                                  defaultValue={tp.unitPrice || 0}
                                  onBlur={(e) => {
                                    const val = parseFloat(e.target.value);
                                    if (!isNaN(val) && val >= 0 && val !== tp.unitPrice) {
                                      handleInlineUpdateTopping(tp.id, tp.name, val);
                                    }
                                  }}
                                  onKeyDown={(e) => {
                                    if (e.key === 'Enter') e.target.blur();
                                  }}
                                  title="Click vào ô để chỉnh sửa đơn giá bán trực tiếp"
                                  style={{
                                    width: '110px',
                                    padding: '5px 10px',
                                    borderRadius: '6px',
                                    border: '1px solid var(--color-border)',
                                    background: '#fff',
                                    fontSize: '0.9rem',
                                  }}
                                />
                                <span>đ</span>
                              </div>
                            </td>
                            <td style={{ padding: '12px 16px' }}>
                              <span style={{ padding: '3px 10px', borderRadius: '6px', fontSize: '0.9rem', fontWeight: 700, background: '#f0fdf4', color: '#15803d' }}>
                                {tp.stock ?? 0}
                              </span>
                            </td>
                            <td style={{ padding: '12px 16px' }}>
                              <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: tp.status ? '#f0fdf4' : '#fef2f2', color: tp.status ? '#166534' : '#991b1b' }}>
                                {tp.status ? 'Đang phục vụ' : 'Tạm dừng bán'}
                              </span>
                            </td>
                            <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                              <button
                                className="btn btn-sm btn-outline"
                                onClick={() => handleToggleToppingStatus(tp)}
                                style={{ padding: '4px 10px', borderRadius: '6px' }}
                              >
                                {tp.status ? 'Tạm ẩn' : 'Bật bán'}
                              </button>
                            </td>
                          </tr>
                        ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* Modal Thêm Topping Mới */}
              {showAddToppingModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '400px', maxWidth: '90%', border: '1px solid var(--color-border)' }}>
                    <h3 style={{ fontSize: '1.2rem', marginBottom: '16px' }}>Thêm Topping Mới</h3>
                    <form onSubmit={handleCreateTopping} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Tên Topping</label>
                        <input
                          type="text"
                          required
                          value={newTopping.name}
                          onChange={(e) => setNewTopping({ ...newTopping, name: e.target.value })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: Pudding Trứng"
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Đơn giá bán (VNĐ)</label>
                        <input
                          type="number"
                          required
                          value={newTopping.unitPrice}
                          onChange={(e) => setNewTopping({ ...newTopping, unitPrice: e.target.value })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: 12000"
                        />
                      </div>
                      <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '8px' }}>Lưu Topping</button>
                        <button type="button" className="btn btn-secondary" onClick={() => setShowAddToppingModal(false)} style={{ flex: 1, padding: '8px' }}>Hủy</button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ==================== 5. QUẢN LÝ KHO (TỒN KHO LƯU TRỮ VS ĐANG BÁN & LOG KHO) ==================== */}
          {activeTab === 'inventory' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              {/* Thanh lọc & tìm kiếm & Nút thêm số lượng vào kho */}
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', flex: 1, alignItems: 'center' }}>
                  <input
                    type="text"
                    placeholder="Tìm kiếm mặt hàng kho..."
                    value={inventorySearch}
                    onChange={(e) => setInventorySearch(e.target.value)}
                    style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', minWidth: '240px', outline: 'none' }}
                  />
                  <div style={{ display: 'flex', gap: '6px' }}>
                    {['ALL', 'PRODUCT', 'TOPPING'].map((tp) => (
                      <button
                        key={tp}
                        type="button"
                        className={`btn btn-sm ${inventoryFilter === tp ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setInventoryFilter(tp)}
                        style={{ padding: '6px 14px', borderRadius: '8px' }}
                      >
                        {tp === 'ALL' ? 'Tất cả' : tp === 'PRODUCT' ? 'Sản phẩm' : 'Topping'}
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  className="btn btn-primary"
                  onClick={() => {
                    setQuickAddItemType('PRODUCT');
                    setQuickAddSelectedId(safeProducts[0]?.id ? String(safeProducts[0].id) : '');
                    setQuickAddAmount('');
                    setQuickAddReason('');
                    setShowQuickAddModal(true);
                  }}
                  style={{ borderRadius: '8px', padding: '8px 18px', fontWeight: 600 }}
                >
                  + Thêm Số Lượng Hàng Vào Kho
                </button>
              </div>

              {/* BẢNG TỔNG HỢP SỐ LƯỢNG KHO */}
              <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--color-border)' }}>
                <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--color-border)', background: '#fafaf9', fontWeight: 700, fontSize: '1rem' }}>
                  Bảng Trạng Thái Số Lượng Kho & Quầy Bán
                </div>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                    <tr>
                      <th style={{ padding: '12px 16px' }}>Tên Mặt Hàng</th>
                      <th style={{ padding: '12px 16px' }}>Phân Loại</th>
                      <th style={{ padding: '12px 16px' }}>Số Lượng Tồn Kho (Kho lưu trữ)</th>
                      <th style={{ padding: '12px 16px' }}>Số Lượng Đang Bán (Menu / Quầy)</th>
                      <th style={{ padding: '12px 16px' }}>Cảnh Báo Tồn</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right' }}>Thao Tác Kho</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeInventoryItems.length === 0 ? (
                      <tr>
                        <td colSpan={6} style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
                          Chưa có dữ liệu tồn kho nào.
                        </td>
                      </tr>
                    ) : (
                      safeInventoryItems
                        .filter((it) => inventoryFilter === 'ALL' || it.itemType === inventoryFilter)
                        .filter((it) => it.itemName?.toLowerCase().includes(inventorySearch.toLowerCase()))
                        .map((item) => {
                          const isLowStock = (item.stock || 0) <= (item.minThreshold || 5);
                          return (
                            <tr key={`${item.itemType}-${item.itemId}`} style={{ borderBottom: '1px solid var(--color-border)' }}>
                              <td style={{ padding: '12px 16px', fontWeight: 600 }}>{item.itemName}</td>
                              <td style={{ padding: '12px 16px' }}>
                                <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: item.itemType === 'PRODUCT' ? '#e0f2fe' : '#fce7f3', color: item.itemType === 'PRODUCT' ? '#0369a1' : '#be185d' }}>
                                  {item.itemType === 'PRODUCT' ? 'Sản phẩm' : 'Topping'}
                                </span>
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                <span style={{ padding: '4px 10px', borderRadius: '6px', fontSize: '0.95rem', fontWeight: 700, background: '#eff6ff', color: '#1d4ed8' }}>
                                  {item.warehouseStock || 0}
                                </span>
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                <span style={{ padding: '4px 10px', borderRadius: '6px', fontSize: '0.95rem', fontWeight: 700, background: '#f0fdf4', color: '#15803d' }}>
                                  {item.stock || 0}
                                </span>
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: isLowStock ? '#fee2e2' : '#f0fdf4', color: isLowStock ? '#b91c1c' : '#166534' }}>
                                  {isLowStock ? `Sắp hết bán (≤ ${item.minThreshold || 5})` : 'Đủ hàng bán'}
                                </span>
                              </td>
                              <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                                <button
                                  className="btn btn-sm btn-outline"
                                  onClick={() => {
                                    setSelectedInventoryItem(item);
                                    setActionAmount('');
                                    setActionReason('');
                                    setShowAddWarehouseModal(true);
                                  }}
                                  style={{ padding: '4px 10px', borderRadius: '6px', marginRight: '6px' }}
                                >
                                  + Nhập Tồn Kho
                                </button>
                                <button
                                  className="btn btn-sm btn-primary"
                                  onClick={() => {
                                    setSelectedInventoryItem(item);
                                    setActionAmount('');
                                    setShowTransferSellingModal(true);
                                  }}
                                  style={{ padding: '4px 10px', borderRadius: '6px' }}
                                >
                                  → Chuyển Sang Bán
                                </button>
                              </td>
                            </tr>
                          );
                        })
                    )}
                  </tbody>
                </table>
              </div>

              {/* BẢNG NHẬT KÝ BIẾN ĐỘNG KHO */}
              <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--color-border)' }}>
                <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--color-border)', background: '#fafaf9', fontWeight: 700, fontSize: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>Nhật Ký Biến Động Kho</span>
                  <span style={{ fontSize: '0.82rem', color: '#64748b' }}>Tổng: {safeInventoryLogs.length} giao dịch</span>
                </div>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
                  <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                    <tr>
                      <th style={{ padding: '10px 16px' }}>Mã Log</th>
                      <th style={{ padding: '10px 16px' }}>Thời Gian</th>
                      <th style={{ padding: '10px 16px' }}>Mặt Hàng</th>
                      <th style={{ padding: '10px 16px' }}>Loại Biến Động</th>
                      <th style={{ padding: '10px 16px', textAlign: 'right' }}>Số Lượng Thay Đổi</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeInventoryLogs.length === 0 ? (
                      <tr>
                        <td colSpan={5} style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
                          Chưa có nhật ký biến động kho nào được ghi nhận.
                        </td>
                      </tr>
                    ) : (
                      safeInventoryLogs.slice(0, 50).map((log) => {
                        const isPlus = (log.quantityChange || 0) > 0;
                        return (
                          <tr key={log.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                            <td style={{ padding: '10px 16px', color: '#64748b' }}>#{log.id}</td>
                            <td style={{ padding: '10px 16px' }}>{formatService.formatDateTime(log.createdAt)}</td>
                            <td style={{ padding: '10px 16px', fontWeight: 600 }}>
                              {log.productName || log.toppingName || 'Mặt hàng'}
                            </td>
                            <td style={{ padding: '10px 16px' }}>
                              <span
                                style={{
                                  padding: '3px 8px',
                                  borderRadius: '6px',
                                  fontSize: '0.78rem',
                                  fontWeight: 600,
                                  background: log.changeType?.includes('HOÀN') || log.changeType?.includes('HỦY')
                                    ? '#fef3c7'
                                    : log.changeType?.includes('CHUYỂN')
                                    ? '#eff6ff'
                                    : log.changeType?.includes('NHẬP')
                                    ? '#f0fdf4'
                                    : '#fef2f2',
                                  color: log.changeType?.includes('HOÀN') || log.changeType?.includes('HỦY')
                                    ? '#b45309'
                                    : log.changeType?.includes('CHUYỂN')
                                    ? '#1d4ed8'
                                    : log.changeType?.includes('NHẬP')
                                    ? '#15803d'
                                    : '#b91c1c',
                                }}
                              >
                                {log.changeType || 'BIẾN ĐỘNG KHO'}
                              </span>
                            </td>
                            <td style={{ padding: '10px 16px', textAlign: 'right', fontWeight: 700, color: isPlus ? '#16a34a' : '#dc2626' }}>
                              {isPlus ? `+${log.quantityChange}` : log.quantityChange}
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>

              {/* MODAL 1: NHẬP THÊM TỒN KHO */}
              {showAddWarehouseModal && selectedInventoryItem && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '420px', maxWidth: '90%', border: '1px solid var(--color-border)' }}>
                    <h3 style={{ fontSize: '1.2rem', marginBottom: '12px' }}>
                      Nhập Thêm Tồn Kho Lưu Trữ
                    </h3>
                    <div style={{ fontSize: '0.9rem', color: '#475569', marginBottom: '16px' }}>
                      Mặt hàng: <strong>{selectedInventoryItem.itemName}</strong> ({selectedInventoryItem.itemType === 'PRODUCT' ? 'Sản phẩm' : 'Topping'})
                      <div style={{ marginTop: '4px', fontSize: '0.85rem' }}>
                        Tồn kho hiện tại: <span style={{ color: '#2563eb', fontWeight: 700 }}>{selectedInventoryItem.warehouseStock || 0}</span>
                      </div>
                    </div>

                    <form onSubmit={handleAddWarehouseStock} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Số lượng nhập thêm vào kho</label>
                        <input
                          type="number"
                          required
                          min="1"
                          value={actionAmount}
                          onChange={(e) => setActionAmount(e.target.value)}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: 50"
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Lý do / Nguồn nhập</label>
                        <input
                          type="text"
                          value={actionReason}
                          onChange={(e) => setActionReason(e.target.value)}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: Nhập hàng từ nhà cung cấp"
                        />
                      </div>
                      <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '8px' }}>Xác Nhận Nhập Kho</button>
                        <button type="button" className="btn btn-secondary" onClick={() => setShowAddWarehouseModal(false)} style={{ flex: 1, padding: '8px' }}>Hủy</button>
                      </div>
                    </form>
                  </div>
                </div>
              )}

              {/* MODAL 2: CHUYỂN SANG ĐANG BÁN (TRỪ TỒN KHO, CỘNG ĐANG BÁN) */}
              {showTransferSellingModal && selectedInventoryItem && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '420px', maxWidth: '90%', border: '1px solid var(--color-border)' }}>
                    <h3 style={{ fontSize: '1.2rem', marginBottom: '12px' }}>
                      Chuyển Sang Số Lượng Đang Bán
                    </h3>
                    <div style={{ fontSize: '0.9rem', color: '#475569', marginBottom: '16px' }}>
                      Mặt hàng: <strong>{selectedInventoryItem.itemName}</strong>
                      <div style={{ display: 'flex', gap: '16px', marginTop: '8px', fontSize: '0.85rem' }}>
                        <div>Tồn kho: <span style={{ color: '#2563eb', fontWeight: 700 }}>{selectedInventoryItem.warehouseStock || 0}</span></div>
                        <div>Đang bán: <span style={{ color: '#16a34a', fontWeight: 700 }}>{selectedInventoryItem.stock || 0}</span></div>
                      </div>
                    </div>

                    <form onSubmit={handleTransferToSelling} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Số lượng đưa ra quầy bán</label>
                        <input
                          type="number"
                          required
                          min="1"
                          max={selectedInventoryItem.warehouseStock || 1}
                          value={actionAmount}
                          onChange={(e) => setActionAmount(e.target.value)}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder={`Tối đa: ${selectedInventoryItem.warehouseStock || 0}`}
                        />
                        <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '4px' }}>
                          * Hệ thống sẽ trừ {actionAmount || 0} từ Tồn kho và cộng {actionAmount || 0} vào số lượng Đang bán trên Menu.
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '8px' }}>Xác Nhận Chuyển Bán</button>
                        <button type="button" className="btn btn-secondary" onClick={() => setShowTransferSellingModal(false)} style={{ flex: 1, padding: '8px' }}>Hủy</button>
                      </div>
                    </form>
                  </div>
                </div>
              )}

              {/* MODAL 3: THÊM SỐ LƯỢNG HÀNG VÀO KHO (QUICK ADD CHO SẢN PHẨM & TOPPING) */}
              {showQuickAddModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '440px', maxWidth: '90%', border: '1px solid var(--color-border)' }}>
                    <h3 style={{ fontSize: '1.2rem', marginBottom: '16px' }}>
                      Thêm Số Lượng Hàng Vào Kho Lưu Trữ
                    </h3>

                    <form onSubmit={handleQuickAddWarehouseStock} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Loại mặt hàng</label>
                        <select
                          value={quickAddItemType}
                          onChange={(e) => {
                            const newType = e.target.value;
                            setQuickAddItemType(newType);
                            if (newType === 'PRODUCT') {
                              setQuickAddSelectedId(safeProducts[0]?.id ? String(safeProducts[0].id) : '');
                            } else {
                              setQuickAddSelectedId(safeToppings[0]?.id ? String(safeToppings[0].id) : '');
                            }
                          }}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                        >
                          <option value="PRODUCT">Sản phẩm (Đồ uống & Bánh)</option>
                          <option value="TOPPING">Topping (Món thêm)</option>
                        </select>
                      </div>

                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Chọn mặt hàng</label>
                        <select
                          value={quickAddSelectedId}
                          onChange={(e) => setQuickAddSelectedId(e.target.value)}
                          required
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                        >
                          <option value="">-- Chọn mặt hàng --</option>
                          {quickAddItemType === 'PRODUCT'
                            ? safeProducts.map((p) => (
                                <option key={p.id} value={p.id}>
                                  #{p.id} - {p.name}
                                </option>
                              ))
                            : safeToppings.map((t) => (
                                <option key={t.id} value={t.id}>
                                  #{t.id} - {t.name}
                                </option>
                              ))}
                        </select>
                      </div>

                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Số lượng nhập thêm vào kho</label>
                        <input
                          type="number"
                          required
                          min="1"
                          value={quickAddAmount}
                          onChange={(e) => setQuickAddAmount(e.target.value)}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: 100"
                        />
                      </div>

                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Lý do / Nguồn nhập</label>
                        <input
                          type="text"
                          value={quickAddReason}
                          onChange={(e) => setQuickAddReason(e.target.value)}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          placeholder="Ví dụ: Nhập hàng từ nhà cung cấp"
                        />
                      </div>

                      <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '8px' }}>Xác Nhận Nhập Kho</button>
                        <button type="button" className="btn btn-secondary" onClick={() => setShowQuickAddModal(false)} style={{ flex: 1, padding: '8px' }}>Hủy</button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ==================== QUẢN LÝ KHUYẾN MÃI (PROMOTIONS) ==================== */}
          {activeTab === 'promotions' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flex: 1, minWidth: '280px' }}>
                  <input
                    type="text"
                    placeholder="Tìm theo mã khuyến mãi hoặc mô tả..."
                    value={promotionSearch}
                    onChange={(e) => setPromotionSearch(e.target.value)}
                    style={{
                      padding: '8px 14px',
                      borderRadius: '8px',
                      border: '1px solid var(--color-border)',
                      width: '100%',
                      maxWidth: '380px',
                    }}
                  />
                </div>

                <button
                  className="btn btn-primary"
                  onClick={handleOpenAddPromotion}
                  style={{ borderRadius: '8px', padding: '8px 18px', display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  + Thêm Mã Khuyến Mãi Mới
                </button>
              </div>

              {/* Bảng danh sách mã khuyến mãi */}
              <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                    <tr>
                      <th style={{ padding: '12px 16px' }}>Mã Khuyến Mãi</th>
                      <th style={{ padding: '12px 16px' }}>Mô Tả Chương Trình</th>
                      <th style={{ padding: '12px 16px' }}>Mức Giảm</th>
                      <th style={{ padding: '12px 16px' }}>Đơn Tối Thiểu</th>
                      <th style={{ padding: '12px 16px' }}>Lượt Đã Dùng / Giới Hạn</th>
                      <th style={{ padding: '12px 16px' }}>Thời Gian Áp Dụng</th>
                      <th style={{ padding: '12px 16px' }}>Trạng Thái</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right' }}>Thao Tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safePromotions.length === 0 ? (
                      <tr>
                        <td colSpan={8} style={{ padding: '36px', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
                          Chưa có mã khuyến mãi nào. Hãy nhấn "+ Thêm Mã Khuyến Mãi Mới" để tạo mã voucher đầu tiên!
                        </td>
                      </tr>
                    ) : (
                      safePromotions
                        .filter((p) => {
                          const q = promotionSearch.toLowerCase();
                          return p.code?.toLowerCase().includes(q) || p.description?.toLowerCase().includes(q);
                        })
                        .map((promo) => {
                          const now = new Date();
                          const isExpired = promo.endDate && new Date(promo.endDate) < now;
                          const isNotStarted = promo.startDate && new Date(promo.startDate) > now;
                          const isMaxedOut = promo.usageLimit && promo.usedCount >= promo.usageLimit;

                          let statusBg = '#f0fdf4';
                          let statusColor = '#166534';
                          let statusText = 'Đang hoạt động';

                          if (!promo.active) {
                            statusBg = '#f1f5f9';
                            statusColor = '#475569';
                            statusText = 'Tạm dừng';
                          } else if (isExpired) {
                            statusBg = '#fef2f2';
                            statusColor = '#991b1b';
                            statusText = 'Đã hết hạn';
                          } else if (isMaxedOut) {
                            statusBg = '#fffbeb';
                            statusColor = '#b45309';
                            statusText = 'Hết lượt dùng';
                          } else if (isNotStarted) {
                            statusBg = '#eff6ff';
                            statusColor = '#1d4ed8';
                            statusText = 'Sắp diễn ra';
                          }

                          return (
                            <tr key={promo.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                              <td style={{ padding: '12px 16px' }}>
                                <span
                                  style={{
                                    fontFamily: 'monospace',
                                    fontWeight: 800,
                                    fontSize: '0.95rem',
                                    padding: '3px 8px',
                                    background: '#fef3c7',
                                    color: '#92400e',
                                    borderRadius: '6px',
                                    border: '1px solid #fde68a',
                                  }}
                                >
                                  {promo.code}
                                </span>
                              </td>
                              <td style={{ padding: '12px 16px', maxWidth: '240px' }}>
                                <div style={{ fontWeight: 600 }}>{promo.description || 'Không có mô tả'}</div>
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                <span style={{ fontWeight: 700, color: 'var(--color-primary)' }}>
                                  {promo.discountType === 'PERCENT'
                                    ? `Giảm ${promo.discountValue}%`
                                    : `Giảm ${formatService.formatCurrency(promo.discountValue)}`}
                                </span>
                                {promo.discountType === 'PERCENT' && promo.maxDiscount && (
                                  <div style={{ fontSize: '0.78rem', color: '#64748b' }}>
                                    Tối đa {formatService.formatCurrency(promo.maxDiscount)}
                                  </div>
                                )}
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                {promo.minOrderAmount
                                  ? formatService.formatCurrency(promo.minOrderAmount)
                                  : 'Không yêu cầu'}
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                <span style={{ fontWeight: 700 }}>{promo.usedCount || 0}</span>
                                <span style={{ color: '#64748b', fontSize: '0.85rem' }}>
                                  {promo.usageLimit ? ` / ${promo.usageLimit} lượt` : ' / Không giới hạn'}
                                </span>
                              </td>
                              <td style={{ padding: '12px 16px', fontSize: '0.85rem', color: '#475569' }}>
                                <div>Từ: {promo.startDate ? String(promo.startDate).split('T')[0] : 'Không đặt'}</div>
                                <div>Đến: {promo.endDate ? String(promo.endDate).split('T')[0] : 'Vô thời hạn'}</div>
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                <span
                                  style={{
                                    padding: '3px 8px',
                                    borderRadius: '6px',
                                    fontSize: '0.8rem',
                                    fontWeight: 600,
                                    background: statusBg,
                                    color: statusColor,
                                  }}
                                >
                                  {statusText}
                                </span>
                              </td>
                              <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                                <div style={{ display: 'inline-flex', gap: '6px' }}>
                                  <button
                                    className="btn btn-sm btn-secondary"
                                    onClick={() => handleOpenEditPromotion(promo)}
                                    style={{ padding: '4px 10px', borderRadius: '6px' }}
                                    title="Chỉnh sửa thông tin khuyến mãi"
                                  >
                                    Sửa
                                  </button>
                                  <button
                                    className="btn btn-sm btn-secondary"
                                    onClick={() => handleTogglePromotionStatus(promo)}
                                    style={{ padding: '4px 10px', borderRadius: '6px' }}
                                    title={promo.active ? 'Tạm ngưng áp dụng mã' : 'Kích hoạt lại mã'}
                                  >
                                    {promo.active ? 'Tắt' : 'Bật'}
                                  </button>
                                  <button
                                    className="btn btn-sm btn-secondary"
                                    onClick={() => handleDeletePromotion(promo)}
                                    style={{ padding: '4px 10px', borderRadius: '6px', color: '#ef4444' }}
                                    title="Xóa mã khuyến mãi"
                                  >
                                    Xóa
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                    )}
                  </tbody>
                </table>
              </div>

              {/* Modal Thêm / Chỉnh Sửa Mã Khuyến Mãi */}
              {showAddPromotionModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px' }}>
                  <div style={{ background: '#fff', borderRadius: '14px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 10px 30px rgba(0,0,0,0.2)', maxHeight: '90vh', overflowY: 'auto' }}>
                    <h3 style={{ marginBottom: '16px', fontSize: '1.2rem', fontWeight: 800 }}>
                      {editingPromotion ? `Chỉnh Sửa Mã Khuyến Mãi: ${editingPromotion.code}` : 'Thêm Mã Khuyến Mãi Mới'}
                    </h3>

                    <form onSubmit={handleSavePromotion} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                          Mã Khuyến Mãi (Voucher Code) *
                        </label>
                        <input
                          type="text"
                          required
                          value={promotionForm.code}
                          onChange={(e) => setPromotionForm({ ...promotionForm, code: e.target.value.toUpperCase() })}
                          placeholder="Ví dụ: GIAM20K, CHAOMUNG, SUMMER10"
                          style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid var(--color-border)', fontFamily: 'monospace', fontWeight: 700 }}
                        />
                      </div>

                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                          Mô Tả Chương Trình
                        </label>
                        <input
                          type="text"
                          value={promotionForm.description}
                          onChange={(e) => setPromotionForm({ ...promotionForm, description: e.target.value })}
                          placeholder="Ví dụ: Giảm 20.000đ cho đơn hàng từ 50.000đ"
                          style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                        />
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Loại Giảm Giá *
                          </label>
                          <select
                            value={promotionForm.discountType}
                            onChange={(e) => setPromotionForm({ ...promotionForm, discountType: e.target.value })}
                            style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          >
                            <option value="PERCENT">Theo Phần Trăm (%)</option>
                            <option value="FIXED">Số Tiền Cố Định (VNĐ)</option>
                          </select>
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Giá Trị Giảm *
                          </label>
                          <input
                            type="number"
                            required
                            min="1"
                            value={promotionForm.discountValue}
                            onChange={(e) => setPromotionForm({ ...promotionForm, discountValue: e.target.value })}
                            placeholder={promotionForm.discountType === 'PERCENT' ? 'Ví dụ: 15 (tức 15%)' : 'Ví dụ: 20000'}
                            style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          />
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Đơn Tối Thiểu (VNĐ)
                          </label>
                          <input
                            type="number"
                            min="0"
                            value={promotionForm.minOrderAmount}
                            onChange={(e) => setPromotionForm({ ...promotionForm, minOrderAmount: e.target.value })}
                            placeholder="Để trống nếu 0đ"
                            style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          />
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Giảm Tối Đa (VNĐ)
                          </label>
                          <input
                            type="number"
                            min="0"
                            disabled={promotionForm.discountType !== 'PERCENT'}
                            value={promotionForm.maxDiscount}
                            onChange={(e) => setPromotionForm({ ...promotionForm, maxDiscount: e.target.value })}
                            placeholder={promotionForm.discountType === 'PERCENT' ? 'Ví dụ: 50000' : 'Chỉ áp dụng với %'}
                            style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)', opacity: promotionForm.discountType !== 'PERCENT' ? 0.5 : 1 }}
                          />
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Ngày Bắt Đầu
                          </label>
                          <input
                            type="date"
                            value={promotionForm.startDate}
                            onChange={(e) => setPromotionForm({ ...promotionForm, startDate: e.target.value })}
                            style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          />
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Ngày Kết Thúc
                          </label>
                          <input
                            type="date"
                            value={promotionForm.endDate}
                            onChange={(e) => setPromotionForm({ ...promotionForm, endDate: e.target.value })}
                            style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          />
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', alignItems: 'center' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Giới Hạn Lượt Dùng
                          </label>
                          <input
                            type="number"
                            min="1"
                            value={promotionForm.usageLimit}
                            onChange={(e) => setPromotionForm({ ...promotionForm, usageLimit: e.target.value })}
                            placeholder="Để trống = Không giới hạn"
                            style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                          />
                        </div>

                        <div style={{ paddingTop: '20px' }}>
                          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.9rem', fontWeight: 600 }}>
                            <input
                              type="checkbox"
                              checked={promotionForm.active}
                              onChange={(e) => setPromotionForm({ ...promotionForm, active: e.target.checked })}
                              style={{ width: '18px', height: '18px' }}
                            />
                            Kích hoạt ngay
                          </label>
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '10px', marginTop: '16px' }}>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '10px' }}>
                          {editingPromotion ? 'Lưu Thay Đổi' : 'Tạo Khuyến Mãi'}
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => {
                            setShowAddPromotionModal(false);
                            setEditingPromotion(null);
                          }}
                          style={{ flex: 1, padding: '10px' }}
                        >
                          Hủy
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ==================== 6. QUẢN LÝ DANH MỤC ==================== */}
          {activeTab === 'categories' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <form onSubmit={handleAddCategory} style={{ display: 'flex', gap: '12px', background: '#fff', padding: '16px', borderRadius: '12px', border: '1px solid var(--color-border)' }}>
                <input
                  type="text"
                  placeholder="Nhập tên danh mục mới..."
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                  style={{ flex: 1, padding: '8px 12px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                />
                <button type="submit" className="btn btn-primary" style={{ padding: '8px 20px' }}>
                  Thêm danh mục
                </button>
              </form>

              <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--color-border)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                    <tr>
                      <th style={{ padding: '12px 16px' }}>Thứ tự</th>
                      <th style={{ padding: '12px 16px' }}>Tên Danh Mục</th>
                      <th style={{ padding: '12px 16px' }}>Trạng Thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeCategories.map((c, i) => (
                      <tr key={c.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                        <td style={{ padding: '12px 16px', color: '#64748b' }}>#{c.displayOrder || i + 1}</td>
                        <td style={{ padding: '12px 16px', fontWeight: 600 }}>{c.name}</td>
                        <td style={{ padding: '12px 16px' }}>
                          <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: '#f0fdf4', color: '#166534' }}>
                            Đang hiển thị
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ==================== 7. QUẢN LÝ NGƯỜI DÙNG ==================== */}
          {activeTab === 'users' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {['ALL', 'ADMIN', 'BARISTA', 'CASHIER', 'CUSTOMER'].map((r) => (
                    <button
                      key={r}
                      className={`btn btn-sm ${userRoleFilter === r ? 'btn-primary' : 'btn-secondary'}`}
                      onClick={() => setUserRoleFilter(r)}
                      style={{ borderRadius: '8px', padding: '6px 14px' }}
                    >
                      {r === 'ALL' ? 'Tất cả vai trò' : r}
                    </button>
                  ))}
                </div>

                {/* Nút Tạo tài khoản mới */}
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => setShowCreateUserModal(true)}
                  style={{ borderRadius: '8px', padding: '8px 16px', fontWeight: 600 }}
                >
                  + Tạo tài khoản mới
                </button>
              </div>

              <div style={{ background: '#fff', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--color-border)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                  <thead style={{ background: '#f8fafc', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                    <tr>
                      <th style={{ padding: '12px 16px' }}>Tên tài khoản</th>
                      <th style={{ padding: '12px 16px' }}>Họ và tên</th>
                      <th style={{ padding: '12px 16px' }}>Email</th>
                      <th style={{ padding: '12px 16px' }}>Số điện thoại</th>
                      <th style={{ padding: '12px 16px' }}>Vai trò</th>
                      <th style={{ padding: '12px 16px' }}>Trạng thái</th>
                      <th style={{ padding: '12px 16px', textAlign: 'center' }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeUsers.length === 0 ? (
                      <tr>
                        <td colSpan={7} style={{ padding: '24px', textAlign: 'center', color: '#94a3b8' }}>
                          Chưa có tài khoản người dùng nào
                        </td>
                      </tr>
                    ) : (
                      safeUsers
                        .filter((u) => userRoleFilter === 'ALL' || u.role === userRoleFilter)
                        .map((u) => (
                          <tr key={u.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                            <td style={{ padding: '12px 16px', fontWeight: 600 }}>{u.username}</td>
                            <td style={{ padding: '12px 16px' }}>{u.fullName || '---'}</td>
                            <td style={{ padding: '12px 16px', color: '#64748b' }}>{u.email}</td>
                            <td style={{ padding: '12px 16px', color: '#64748b' }}>{u.phone || '---'}</td>
                            <td style={{ padding: '12px 16px' }}>
                              <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: '#eff6ff', color: '#1d4ed8', fontWeight: 600 }}>
                                {u.role}
                              </span>
                            </td>
                            <td style={{ padding: '12px 16px' }}>
                              <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: u.status ? '#f0fdf4' : '#fef2f2', color: u.status ? '#166534' : '#991b1b', fontWeight: 600 }}>
                                {u.status ? 'Đang hoạt động' : 'Bị khóa'}
                              </span>
                            </td>
                            <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                              {u.status ? (
                                <button
                                  type="button"
                                  onClick={() => setUserToLock(u)}
                                  style={{
                                    padding: '5px 12px',
                                    borderRadius: '6px',
                                    background: '#dc2626',
                                    color: '#ffffff',
                                    border: 'none',
                                    fontWeight: 700,
                                    fontSize: '0.8rem',
                                    cursor: 'pointer',
                                    boxShadow: '0 2px 4px rgba(220, 38, 38, 0.25)',
                                  }}
                                >
                                  Khóa
                                </button>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() => handleToggleLockUser(u)}
                                  style={{
                                    padding: '5px 12px',
                                    borderRadius: '6px',
                                    background: '#16a34a',
                                    color: '#ffffff',
                                    border: 'none',
                                    fontWeight: 600,
                                    fontSize: '0.8rem',
                                    cursor: 'pointer',
                                  }}
                                >
                                  Mở khóa
                                </button>
                              )}
                            </td>
                          </tr>
                        ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* MODAL XÁC NHẬN KHÓA TÀI KHOẢN (Mục 8 - Nút in đỏ) */}
              {userToLock && (
                <div
                  style={{
                    position: 'fixed',
                    inset: 0,
                    background: 'rgba(0,0,0,0.5)',
                    zIndex: 9999,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '16px',
                  }}
                  onClick={() => setUserToLock(null)}
                >
                  <div
                    style={{
                      background: '#fff',
                      borderRadius: '16px',
                      padding: '28px 24px',
                      maxWidth: '420px',
                      width: '100%',
                      textAlign: 'center',
                      boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div
                      style={{
                        width: '50px',
                        height: '50px',
                        borderRadius: '50%',
                        background: '#fee2e2',
                        color: '#dc2626',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        margin: '0 auto 14px',
                      }}
                    >
                      <IconLock size={28} color="#dc2626" />
                    </div>
                    <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 8px' }}>
                      Xác nhận khóa tài khoản?
                    </h3>
                    <p style={{ color: '#64748b', fontSize: '0.92rem', margin: '0 0 24px', lineHeight: 1.5 }}>
                      Bạn có chắc chắn muốn khóa tài khoản <strong>{userToLock.username}</strong> ({userToLock.fullName || userToLock.role}) không? Sau khi khóa, người dùng sẽ không thể đăng nhập.
                    </p>
                    <div style={{ display: 'flex', gap: '12px' }}>
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => setUserToLock(null)}
                        style={{ flex: 1, padding: '10px' }}
                      >
                        Hủy
                      </button>
                      <button
                        type="button"
                        onClick={() => handleToggleLockUser(userToLock)}
                        style={{
                          flex: 1,
                          padding: '10px',
                          borderRadius: '8px',
                          background: '#dc2626',
                          color: '#fff',
                          border: 'none',
                          fontWeight: 700,
                          cursor: 'pointer',
                        }}
                      >
                        Khóa tài khoản
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* MODAL TẠO TÀI KHOẢN MỚI (Mục 8 - Hỗ trợ đầy đủ role & thông tin) */}
              {showCreateUserModal && (
                <div
                  style={{
                    position: 'fixed',
                    inset: 0,
                    background: 'rgba(0,0,0,0.5)',
                    zIndex: 9999,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '16px',
                  }}
                  onClick={() => setShowCreateUserModal(false)}
                >
                  <div
                    style={{
                      background: '#fff',
                      borderRadius: '16px',
                      maxWidth: '520px',
                      width: '100%',
                      maxHeight: '90vh',
                      overflowY: 'auto',
                      padding: '28px',
                      boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
                      <h3 style={{ fontSize: '1.3rem', fontWeight: 700, margin: 0 }}>
                        Tạo Tài Khoản Người Dùng Mới
                      </h3>
                      <button
                        type="button"
                        onClick={() => setShowCreateUserModal(false)}
                        style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#64748b' }}
                      >
                        ✕
                      </button>
                    </div>

                    <form onSubmit={handleCreateUser} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Tên đăng nhập (Username) *
                          </label>
                          <input
                            type="text"
                            required
                            placeholder="VD: barista_nam"
                            value={newUserForm.username}
                            onChange={(e) => setNewUserForm({ ...newUserForm, username: e.target.value })}
                            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--color-border)', boxSizing: 'border-box' }}
                          />
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Mật khẩu khởi tạo *
                          </label>
                          <input
                            type="password"
                            required
                            placeholder="Mật khẩu tối thiểu 8 ký tự"
                            value={newUserForm.password}
                            onChange={(e) => setNewUserForm({ ...newUserForm, password: e.target.value })}
                            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--color-border)', boxSizing: 'border-box' }}
                          />
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Họ và tên *
                          </label>
                          <input
                            type="text"
                            required
                            placeholder="VD: Nguyễn Văn Nam"
                            value={newUserForm.fullName}
                            onChange={(e) => setNewUserForm({ ...newUserForm, fullName: e.target.value })}
                            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--color-border)', boxSizing: 'border-box' }}
                          />
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Vai trò (Role) *
                          </label>
                          <select
                            value={newUserForm.role}
                            onChange={(e) => setNewUserForm({ ...newUserForm, role: e.target.value })}
                            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--color-border)', background: '#fff', boxSizing: 'border-box', fontWeight: 600 }}
                          >
                            <option value="BARISTA">BARISTA (Pha chế)</option>
                            <option value="CASHIER">CASHIER (Thu ngân)</option>
                            <option value="CUSTOMER">CUSTOMER (Khách hàng)</option>
                            <option value="ADMIN">ADMIN (Quản trị viên)</option>
                          </select>
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Email *
                          </label>
                          <input
                            type="email"
                            required
                            placeholder="nam@brewlite.vn"
                            value={newUserForm.email}
                            onChange={(e) => setNewUserForm({ ...newUserForm, email: e.target.value })}
                            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--color-border)', boxSizing: 'border-box' }}
                          />
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Số điện thoại
                          </label>
                          <input
                            type="tel"
                            placeholder="0912345678"
                            value={newUserForm.phone}
                            onChange={(e) => setNewUserForm({ ...newUserForm, phone: e.target.value })}
                            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--color-border)', boxSizing: 'border-box' }}
                          />
                        </div>
                      </div>

                      {/* Ngày sinh & Giới tính */}
                      <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: '12px' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Ngày sinh (Ngày / Tháng / Năm)
                          </label>
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.2fr 1.3fr', gap: '6px' }}>
                            <input
                              type="text"
                              maxLength={2}
                              placeholder="01"
                              value={newUserForm.dobDay}
                              onChange={(e) => setNewUserForm({ ...newUserForm, dobDay: e.target.value })}
                              style={{ textAlign: 'center', padding: '8px 4px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                            />
                            <input
                              type="text"
                              list="admin-user-dob-months"
                              placeholder="01"
                              value={newUserForm.dobMonth}
                              onChange={(e) => setNewUserForm({ ...newUserForm, dobMonth: e.target.value })}
                              style={{ textAlign: 'center', padding: '8px 4px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                            />
                            <datalist id="admin-user-dob-months">
                              {Array.from({ length: 12 }, (_, i) => {
                                const m = String(i + 1).padStart(2, '0');
                                return <option key={m} value={m}>{`Tháng ${m}`}</option>;
                              })}
                            </datalist>
                            <input
                              type="text"
                              maxLength={4}
                              placeholder="2000"
                              value={newUserForm.dobYear}
                              onChange={(e) => setNewUserForm({ ...newUserForm, dobYear: e.target.value })}
                              style={{ textAlign: 'center', padding: '8px 4px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                            />
                          </div>
                        </div>

                        <div>
                          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                            Giới tính
                          </label>
                          <select
                            value={newUserForm.gender ? 'true' : 'false'}
                            onChange={(e) => setNewUserForm({ ...newUserForm, gender: e.target.value === 'true' })}
                            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--color-border)', background: '#fff', boxSizing: 'border-box' }}
                          >
                            <option value="true">Nam</option>
                            <option value="false">Nữ</option>
                          </select>
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => setShowCreateUserModal(false)}
                          style={{ flex: 1, padding: '10px' }}
                        >
                          Hủy
                        </button>
                        <button
                          type="submit"
                          className="btn btn-primary"
                          style={{ flex: 1, padding: '10px', fontWeight: 600 }}
                        >
                          Tạo tài khoản
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ==================== 8. THỜI TIẾT ==================== */}
          {activeTab === 'weather' && (
            <div style={{ maxWidth: '640px' }}>
              <WeatherWidget />
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default AdminPortalPage;
