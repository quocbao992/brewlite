import React, { useState, useEffect } from 'react';
import { orderApi, productApi, categoryApi, userApi, toppingApi, inventoryApi, promotionApi } from '../api';
import { formatService } from '../services/formatService';
import LoadingSpinner from '../components/common/LoadingSpinner';
import WeatherWidget from '../components/common/WeatherWidget';
import { useAuth } from '../context/AuthContext';
import { IconLock, IconClock, IconClose, IconAlert } from '../components/common/Icons';

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
  const [productStatusFilter, setProductStatusFilter] = useState('ALL');
  const [newProduct, setNewProduct] = useState({ name: '', basePrice: '', categoryId: '', isAvailable: true });
  const [showAddProductModal, setShowAddProductModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);

  // 3. Topping 
  const [toppings, setToppings] = useState([]);
  const [toppingSearch, setToppingSearch] = useState('');
  const [toppingStatusFilter, setToppingStatusFilter] = useState('ALL');
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
  const [categoryToDelete, setCategoryToDelete] = useState(null);
  const [productsInCategory,setProductsInCategory] = useState([]);
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
  const [promotionToDelete, setPromotionToDelete] = useState(null);
  const [deletingPromotion, setDeletingPromotion] = useState(false);
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

  const [statPeriod, setStatPeriod] = useState('WEEK');
  const [hoveredPoint, setHoveredPoint] = useState(null);

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
      const currentStatus = (prod.isAvailable ?? prod.status) !== false;
      const updatedStatus = !currentStatus;
      await productApi.updateStatus(prod.id, updatedStatus);
      setProducts((prev) => (Array.isArray(prev) ? prev.map((p) => (p.id === prod.id ? { ...p, isAvailable: updatedStatus, status: updatedStatus } : p)) : []));
      showNotification(`Đã ${updatedStatus ? 'bật bán' : 'tạm dừng bán'} ${prod.name}`);
      loadDataForTab('products');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi cập nhật trạng thái', 'error');
    }
  };

  const handleSaveProductEdit = async (e) => {
    e.preventDefault();
    if (!editingProduct || !editingProduct.name.trim()) return;
    try {
      const priceNum = parseFloat(editingProduct.unitPrice ?? editingProduct.basePrice);
      if (isNaN(priceNum) || priceNum <= 0) {
        showNotification('Giá sản phẩm phải lớn hơn 0', 'error');
        return;
      }
      const payload = {
        name: editingProduct.name.trim(),
        categoryId: Number(editingProduct.categoryId),
        unitPrice: priceNum,
        basePrice: priceNum,
        status: editingProduct.status ?? editingProduct.isAvailable ?? true,
        imgURL: editingProduct.imgURL || '',
      };
      await productApi.update(editingProduct.id, payload);
      const targetCat = safeCategories.find((c) => String(c.id) === String(payload.categoryId));
      setProducts((prev) =>
        Array.isArray(prev)
          ? prev.map((p) => (p.id === editingProduct.id ? { ...p, ...payload, categoryName: targetCat?.name || p.categoryName } : p))
          : []
      );
      showNotification(`Đã cập nhật sản phẩm: ${editingProduct.name}`);
      setEditingProduct(null);
      loadDataForTab('products');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi cập nhật sản phẩm', 'error');
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
      const priceNum = parseFloat(newProduct.basePrice);
      await productApi.create({
        name: newProduct.name,
        basePrice: priceNum,
        unitPrice: priceNum,
        categoryId: newProduct.categoryId || categories[0]?.id || 1,
        status: true,
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
    if (!selectedInventoryItem) return;
    const amountNum = parseInt(actionAmount, 10);
    if (isNaN(amountNum) || amountNum <= 0) {
      showNotification('Số lượng nhập thêm vào kho phải lớn hơn hoặc bằng 1', 'error');
      return;
    }

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
    if (!selectedInventoryItem) return;
    const amountNum = parseInt(actionAmount, 10);
    if (isNaN(amountNum) || amountNum <= 0) {
      showNotification('Số lượng đưa ra quầy bán phải lớn hơn hoặc bằng 1', 'error');
      return;
    }

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
    if (!newCategoryName.trim()) {
      showNotification('Vui lòng nhập tên danh mục mới', 'error');
      return;
    }
    try {
      await categoryApi.create({ name: newCategoryName.trim(), displayOrder: categories.length + 1 });
      showNotification(`Đã thêm danh mục: ${newCategoryName}`);
      setNewCategoryName('');
      loadDataForTab('categories');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi tạo danh mục', 'error');
    }
  };

  const handleToggleCategoryStatus = async (category) => {
    try {
      if(category.status !== false) {
        await categoryApi.hide(category.id);
        showNotification(`Đã tạm ẩn danh mục: ${category.name}`);
      } else {
        await categoryApi.show(category.id);
        showNotification(`Đã hiện thị danh mục: ${category.name}`);
      }
      loadDataForTab('categories');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi cập nhật trạng thái','error');
    }
  };
  const handleOpenDeleteCategory = (category) => {
    const prods = (products || []).filter(
      (p) => p.categoryId === category.id || p.category?.id === category.id
    );
    setCategoryToDelete(category);
    setProductsInCategory(prods);
  };
  const handleConfirmDeleteCategory = async () => {
    if (!categoryToDelete) return;
    try {
      await categoryApi.delete(categoryToDelete.id);
      showNotification(`Đã xóa danh mục: ${categoryToDelete.name}`);
      setCategoryToDelete(null);
      setProductsInCategory([]);
      loadDataForTab('categories');
    } catch (err) {
      showNotification(err.message || 'Lỗi khi xóa danh mục', 'error');
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

  const handleConfirmDeletePromotion = async () => {
    if (!promotionToDelete) return;
    setDeletingPromotion(true);
    try {
      await promotionApi.delete(promotionToDelete.id);
      showNotification(`Đã xóa vĩnh viễn mã khuyến mãi ${promotionToDelete.code}`);
      setPromotionToDelete(null);
      loadDataForTab('promotions');
    } catch (err) {
      showNotification(err.message || 'Lỗi xóa mã khuyến mãi', 'error');
    } finally {
      setDeletingPromotion(false);
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

  const paidOrders = safeOrders.filter((o) =>
    ['PAID', 'PREPARING', 'READY', 'COMPLETED'].includes(o?.status)
  );

  const now = new Date();

  let statLabels = [];
  let statValues = [];
  let periodOrders = [];

  if (statPeriod === 'WEEK') {
    statLabels = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật'];
    statValues = [0, 0, 0, 0, 0, 0, 0];

    const currentDay = now.getDay();
    const distanceToMonday = (currentDay + 6) % 7;
    const monday = new Date(now);
    monday.setDate(now.getDate() - distanceToMonday);
    monday.setHours(0, 0, 0, 0);

    const endOfWeek = new Date(monday);
    endOfWeek.setDate(monday.getDate() + 7);

    paidOrders.forEach((o) => {
      const d = o?.createdAt ? formatService.parseDate(o.createdAt) : null;
      if (d && d >= monday && d < endOfWeek) {
        periodOrders.push(o);
        const dayIdx = (d.getDay() + 6) % 7;
        statValues[dayIdx] += Number(o?.totalAmount || 0);
      }
    });

    if (periodOrders.length === 0) {
      paidOrders.forEach((o) => {
        periodOrders.push(o);
        const d = o?.createdAt ? formatService.parseDate(o.createdAt) : new Date();
        const dayIdx = (d.getDay() + 6) % 7;
        statValues[dayIdx] += Number(o?.totalAmount || 0);
      });
    }
  } else if (statPeriod === 'MONTH') {
    statLabels = ['Tuần 1 (1-7)', 'Tuần 2 (8-14)', 'Tuần 3 (15-21)', 'Tuần 4 (22+)'];
    statValues = [0, 0, 0, 0];

    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth();

    paidOrders.forEach((o) => {
      const d = o?.createdAt ? formatService.parseDate(o.createdAt) : null;
      if (d && d.getFullYear() === currentYear && d.getMonth() === currentMonth) {
        periodOrders.push(o);
        const dateNum = d.getDate();
        if (dateNum <= 7) statValues[0] += Number(o?.totalAmount || 0);
        else if (dateNum <= 14) statValues[1] += Number(o?.totalAmount || 0);
        else if (dateNum <= 21) statValues[2] += Number(o?.totalAmount || 0);
        else statValues[3] += Number(o?.totalAmount || 0);
      }
    });

    if (periodOrders.length === 0) {
      paidOrders.forEach((o) => {
        periodOrders.push(o);
        const d = o?.createdAt ? formatService.parseDate(o.createdAt) : new Date();
        const dateNum = d.getDate();
        if (dateNum <= 7) statValues[0] += Number(o?.totalAmount || 0);
        else if (dateNum <= 14) statValues[1] += Number(o?.totalAmount || 0);
        else if (dateNum <= 21) statValues[2] += Number(o?.totalAmount || 0);
        else statValues[3] += Number(o?.totalAmount || 0);
      });
    }
  } else {
    statLabels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
    statValues = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

    const currentYear = now.getFullYear();

    paidOrders.forEach((o) => {
      const d = o?.createdAt ? formatService.parseDate(o.createdAt) : null;
      if (d && d.getFullYear() === currentYear) {
        periodOrders.push(o);
        statValues[d.getMonth()] += Number(o?.totalAmount || 0);
      }
    });

    if (periodOrders.length === 0) {
      paidOrders.forEach((o) => {
        periodOrders.push(o);
        const d = o?.createdAt ? formatService.parseDate(o.createdAt) : new Date();
        statValues[d.getMonth()] += Number(o?.totalAmount || 0);
      });
    }
  }

  const periodRevenue = statValues.reduce((a, b) => a + b, 0);
  const periodOrderCount = periodOrders.length;
  const periodAvgOrder = periodOrderCount > 0 ? Math.round(periodRevenue / periodOrderCount) : 0;

  const productCategoryMap = {};
  safeProducts.forEach((p) => {
    if (p.id) productCategoryMap[p.id] = p.categoryId;
  });
  const categoryNameMap = {};
  safeCategories.forEach((c) => {
    if (c.id) categoryNameMap[c.id] = c.name;
  });

  const categoryRevenueMap = {};
  periodOrders.forEach((o) => {
    if (Array.isArray(o?.items) && o.items.length > 0) {
      o.items.forEach((item) => {
        const catId = productCategoryMap[item.productId];
        const catName = categoryNameMap[catId] || 'Thức uống khác';
        categoryRevenueMap[catName] = (categoryRevenueMap[catName] || 0) + Number(item.lineTotal || 0);
      });
    } else {
      const fallbackCat = o?.orderType === 'DINE_IN' ? 'Tại bàn' : 'Mang đi';
      categoryRevenueMap[fallbackCat] = (categoryRevenueMap[fallbackCat] || 0) + Number(o?.totalAmount || 0);
    }
  });

  const donutColors = ['#c17838', '#2563eb', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4'];
  const donutEntries = Object.entries(categoryRevenueMap)
    .map(([name, amount], idx) => ({
      name,
      amount,
      color: donutColors[idx % donutColors.length],
    }))
    .filter((e) => e.amount > 0)
    .sort((a, b) => b.amount - a.amount);

  const donutTotal = donutEntries.reduce((sum, e) => sum + e.amount, 0) || periodRevenue || 1;
  let cumulativePercent = 0;
  const donutSlices = donutEntries.map((e) => {
    const percent = e.amount / donutTotal;
    const start = cumulativePercent;
    cumulativePercent += percent;
    return {
      ...e,
      percent,
      start,
    };
  });

  const maxStatVal = Math.max(...statValues, 50000) * 1.25;
  const svgWidth = 620;
  const svgHeight = 200;
  const padLeft = 50;
  const padRight = 25;
  const padTop = 25;
  const padBottom = 30;
  const chartW = svgWidth - padLeft - padRight;
  const chartH = svgHeight - padTop - padBottom;

  const splinePoints = statValues.map((val, idx) => {
    const x = padLeft + (idx / Math.max(1, statValues.length - 1)) * chartW;
    const y = padTop + chartH - (val / maxStatVal) * chartH;
    return { x, y, val, label: statLabels[idx] };
  });

  let splinePath = '';
  if (splinePoints.length > 0) {
    splinePath = `M ${splinePoints[0].x.toFixed(1)} ${splinePoints[0].y.toFixed(1)}`;
    for (let i = 0; i < splinePoints.length - 1; i++) {
      const p0 = i > 0 ? splinePoints[i - 1] : splinePoints[i];
      const p1 = splinePoints[i];
      const p2 = splinePoints[i + 1];
      const p3 = i < splinePoints.length - 2 ? splinePoints[i + 2] : p2;

      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;

      splinePath += ` C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
    }
  }

  const areaBottomY = padTop + chartH;
  const splineAreaPath = splinePoints.length > 0
    ? `${splinePath} L ${splinePoints[splinePoints.length - 1].x.toFixed(1)} ${areaBottomY} L ${splinePoints[0].x.toFixed(1)} ${areaBottomY} Z`
    : '';

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

              <div
                style={{
                  background: '#ffffff',
                  borderRadius: '14px',
                  padding: '24px',
                  border: '1px solid var(--color-border)',
                  boxShadow: 'var(--shadow-sm)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '20px',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '14px',
                    borderBottom: '1px solid #f1f5f9',
                    paddingBottom: '16px',
                  }}
                >
                  <div>
                    <h3 style={{ fontSize: '1.18rem', fontWeight: 800, color: '#1e293b', margin: 0 }}>
                      Báo Cáo & Phân Tích Doanh Thu
                    </h3>
                    <div style={{ fontSize: '0.84rem', color: '#64748b', marginTop: '4px' }}>
                      Theo dõi dòng tiền thực tế và xu hướng tăng trưởng qua biểu đồ
                    </div>
                  </div>

                  <div style={{ display: 'inline-flex', background: '#f1f5f9', padding: '4px', borderRadius: '10px', gap: '4px' }}>
                    {[
                      { id: 'WEEK', label: 'Tuần này' },
                      { id: 'MONTH', label: 'Tháng này' },
                      { id: 'YEAR', label: 'Năm nay' },
                    ].map((btn) => (
                      <button
                        key={btn.id}
                        type="button"
                        onClick={() => setStatPeriod(btn.id)}
                        style={{
                          padding: '6px 16px',
                          borderRadius: '8px',
                          border: 'none',
                          fontSize: '0.85rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                          background: statPeriod === btn.id ? '#ffffff' : 'transparent',
                          color: statPeriod === btn.id ? 'var(--color-accent)' : '#64748b',
                          boxShadow: statPeriod === btn.id ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                          transition: 'all 0.15s ease',
                        }}
                      >
                        {btn.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px' }}>
                  <div style={{ background: '#fef3c7', padding: '14px 18px', borderRadius: '10px', border: '1px solid #fde68a' }}>
                    <div style={{ fontSize: '0.78rem', color: '#92400e', fontWeight: 700, textTransform: 'uppercase' }}>
                      Doanh thu ({statPeriod === 'WEEK' ? 'Tuần này' : statPeriod === 'MONTH' ? 'Tháng này' : 'Năm nay'})
                    </div>
                    <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#78350f', marginTop: '4px' }}>
                      {formatService.formatCurrency(periodRevenue)}
                    </div>
                  </div>

                  <div style={{ background: '#ecfdf5', padding: '14px 18px', borderRadius: '10px', border: '1px solid #a7f3d0' }}>
                    <div style={{ fontSize: '0.78rem', color: '#065f46', fontWeight: 700, textTransform: 'uppercase' }}>
                      Đơn hàng thành công
                    </div>
                    <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#047857', marginTop: '4px' }}>
                      {periodOrderCount} <span style={{ fontSize: '0.88rem', fontWeight: 600 }}>đơn</span>
                    </div>
                  </div>

                  <div style={{ background: '#eff6ff', padding: '14px 18px', borderRadius: '10px', border: '1px solid #bfdbfe' }}>
                    <div style={{ fontSize: '0.78rem', color: '#1e40af', fontWeight: 700, textTransform: 'uppercase' }}>
                      Giá trị trung bình / đơn (AOV)
                    </div>
                    <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#1d4ed8', marginTop: '4px' }}>
                      {formatService.formatCurrency(periodAvgOrder)}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px', alignItems: 'stretch' }}>
                  <div
                    style={{
                      border: '1px solid var(--color-border)',
                      borderRadius: '12px',
                      padding: '18px 20px',
                      background: '#ffffff',
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '0.98rem', color: '#1e293b' }}>
                          Biểu đồ xu hướng doanh thu
                        </div>
                        <div style={{ fontSize: '0.78rem', color: '#64748b' }}>
                          Biến động doanh thu theo {statPeriod === 'WEEK' ? 'ngày trong tuần' : statPeriod === 'MONTH' ? 'các tuần trong tháng' : 'các tháng trong năm'}
                        </div>
                      </div>
                      <span style={{ fontSize: '0.76rem', padding: '3px 8px', borderRadius: '6px', background: '#fef3c7', color: '#92400e', fontWeight: 700 }}>
                        Đường cong mềm
                      </span>
                    </div>

                    <div style={{ width: '100%', position: 'relative', overflowX: 'auto' }}>
                      <svg
                        viewBox={`0 0 ${svgWidth} ${svgHeight}`}
                        style={{ width: '100%', height: 'auto', display: 'block' }}
                      >
                        <defs>
                          <linearGradient id="splineAreaGradient" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor="#c17838" stopOpacity="0.4" />
                            <stop offset="70%" stopColor="#c17838" stopOpacity="0.08" />
                            <stop offset="100%" stopColor="#c17838" stopOpacity="0" />
                          </linearGradient>
                        </defs>

                        {[0, 0.25, 0.5, 0.75, 1].map((pct, idx) => {
                          const y = padTop + chartH * (1 - pct);
                          const val = Math.round(maxStatVal * pct);
                          return (
                            <g key={idx}>
                              <line
                                x1={padLeft}
                                y1={y}
                                x2={padLeft + chartW}
                                y2={y}
                                stroke="#f1f5f9"
                                strokeDasharray={pct === 0 ? '0' : '4 4'}
                                strokeWidth="1"
                              />
                              <text
                                x={padLeft - 6}
                                y={y + 3}
                                textAnchor="end"
                                fontSize="9"
                                fill="#94a3b8"
                                fontFamily="sans-serif"
                              >
                                {val >= 1000000 ? `${(val / 1000000).toFixed(1)}Tr` : val >= 1000 ? `${Math.round(val / 1000)}k` : val}
                              </text>
                            </g>
                          );
                        })}

                        {splineAreaPath && (
                          <path
                            d={splineAreaPath}
                            fill="url(#splineAreaGradient)"
                          />
                        )}

                        {splinePath && (
                          <path
                            d={splinePath}
                            fill="none"
                            stroke="#c17838"
                            strokeWidth="3.2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        )}

                        {splinePoints.map((pt, idx) => {
                          const isHovered = hoveredPoint === idx;
                          return (
                            <g
                              key={idx}
                              onMouseEnter={() => setHoveredPoint(idx)}
                              onMouseLeave={() => setHoveredPoint(null)}
                              style={{ cursor: 'pointer' }}
                            >
                              {isHovered && (
                                <line
                                  x1={pt.x}
                                  y1={padTop}
                                  x2={pt.x}
                                  y2={areaBottomY}
                                  stroke="#fed7aa"
                                  strokeWidth="1.5"
                                  strokeDasharray="3 3"
                                />
                              )}

                              <circle
                                cx={pt.x}
                                cy={pt.y}
                                r={isHovered ? 7 : 4.5}
                                fill="#ffffff"
                                stroke="#c17838"
                                strokeWidth={isHovered ? 3 : 2.5}
                                style={{ transition: 'all 0.15s ease' }}
                              />

                              {isHovered && (
                                <g transform={`translate(${pt.x}, ${Math.max(16, pt.y - 12)})`}>
                                  <rect
                                    x="-48"
                                    y="-22"
                                    width="96"
                                    height="20"
                                    rx="5"
                                    fill="#1e293b"
                                    filter="drop-shadow(0 2px 4px rgba(0,0,0,0.2))"
                                  />
                                  <text
                                    x="0"
                                    y="-8"
                                    textAnchor="middle"
                                    fontSize="10"
                                    fill="#ffffff"
                                    fontWeight="700"
                                  >
                                    {formatService.formatCurrency(pt.val)}
                                  </text>
                                </g>
                              )}

                              <text
                                x={pt.x}
                                y={areaBottomY + 18}
                                textAnchor="middle"
                                fontSize="10"
                                fill={isHovered ? '#c17838' : '#64748b'}
                                fontWeight={isHovered ? 700 : 500}
                              >
                                {pt.label}
                              </text>
                            </g>
                          );
                        })}
                      </svg>
                    </div>
                  </div>

                  <div
                    style={{
                      border: '1px solid var(--color-border)',
                      borderRadius: '12px',
                      padding: '18px 20px',
                      background: '#ffffff',
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    <div style={{ marginBottom: '14px' }}>
                      <div style={{ fontWeight: 700, fontSize: '0.98rem', color: '#1e293b' }}>
                        Biểu đồ hình tròn cơ cấu doanh thu
                      </div>
                      <div style={{ fontSize: '0.78rem', color: '#64748b' }}>
                        Tỷ trọng đóng góp theo nhóm sản phẩm & thức uống
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '20px', flexWrap: 'wrap', flex: 1 }}>
                      <div style={{ width: '190px', height: '190px', position: 'relative', flexShrink: 0 }}>
                        <svg viewBox="0 0 210 210" style={{ width: '100%', height: '100%', transform: 'rotate(-90deg)' }}>
                          <circle
                            cx="105"
                            cy="105"
                            r="68"
                            fill="none"
                            stroke="#f1f5f9"
                            strokeWidth="26"
                          />

                          {donutSlices.map((slice, idx) => {
                            const circumference = 2 * Math.PI * 68;
                            return (
                              <circle
                                key={idx}
                                cx="105"
                                cy="105"
                                r="68"
                                fill="none"
                                stroke={slice.color}
                                strokeWidth="26"
                                strokeDasharray={`${slice.percent * circumference} ${circumference}`}
                                strokeDashoffset={-slice.start * circumference}
                                strokeLinecap={slice.percent >= 0.99 ? 'butt' : 'round'}
                                style={{ transition: 'stroke-dasharray 0.3s ease' }}
                              />
                            );
                          })}
                        </svg>

                        <div
                          style={{
                            position: 'absolute',
                            inset: 0,
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            pointerEvents: 'none',
                            textAlign: 'center',
                            padding: '10px',
                          }}
                        >
                          <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase' }}>
                            DOANH THU
                          </div>
                          <div style={{ fontSize: '0.98rem', fontWeight: 800, color: '#1e293b', marginTop: '2px' }}>
                            {formatService.formatCurrency(periodRevenue)}
                          </div>
                        </div>
                      </div>

                      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', minWidth: '160px', flex: 1 }}>
                        {donutSlices.length === 0 ? (
                          <div style={{ fontSize: '0.85rem', color: '#94a3b8', fontStyle: 'italic' }}>
                            Chưa có dữ liệu cơ cấu cho kỳ này
                          </div>
                        ) : (
                          donutSlices.slice(0, 5).map((slice, idx) => (
                            <div key={idx} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.84rem' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: slice.color, display: 'inline-block' }} />
                                <span style={{ color: '#334155', fontWeight: 600 }}>{slice.name}</span>
                              </div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <span style={{ color: '#64748b', fontSize: '0.78rem' }}>{Math.round(slice.percent * 100)}%</span>
                                <span style={{ fontWeight: 700, color: '#1e293b' }}>{formatService.formatCurrency(slice.amount)}</span>
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  </div>
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
                              <strong>{order.orderType === 'DINE_IN' ? `Tại bàn: ${order.tableNumber || 'Chưa gán'}` : 'Mang đi'}</strong>
                              {' • '}{formatService.formatDateTime(order.createdAt)}
                            </div>
                          </div>

                          {order.note && (
                            <div style={{ fontSize: '0.82rem', padding: '6px 10px', background: '#fef3c7', color: '#92400e', borderRadius: '6px' }}>
                              <strong>Ghi chú:</strong> {order.note}
                            </div>
                          )}

                          <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', fontSize: '0.88rem' }}>
                            {Array.isArray(order.items) && order.items.length > 0 ? (
                              order.items.map((it, idx) => (
                                <div key={idx} style={{ padding: '4px 0', borderBottom: idx < order.items.length - 1 ? '1px dashed #e2e8f0' : 'none' }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600 }}>
                                    <span>{it.quantity}x {it.productName || it.name} {it.size ? `(Size ${it.size})` : ''}</span>
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
                              <div style={{ color: '#94a3b8', fontStyle: 'italic', fontSize: '0.82rem', padding: '4px 0' }}>
                                Không có thông tin món chi tiết
                              </div>
                            )}
                            <div style={{ borderTop: '1px solid var(--color-border)', marginTop: '8px', paddingTop: '6px', textAlign: 'right', fontWeight: 700, fontSize: '0.95rem' }}>
                              Tổng đơn: {formatService.formatCurrency(order.totalAmount || 0)}
                            </div>
                          </div>

                          {/* KHỐI HÀNH ĐỘNG CỦA BARISTA THEO TỪNG TRẠNG THÁI */}
                          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: 'auto', paddingTop: '8px' }}>
                            {isPending && (
                              <div style={{ display: 'flex', width: '100%', gap: '8px', alignItems: 'center' }}>
                                <span style={{ fontSize: '0.82rem', color: '#b45309', fontWeight: 600, flex: 1, display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                  <IconClock size={16} /> Chờ khách thanh toán qua cổng...
                                </span>
                                <button
                                  type="button"
                                  className="btn btn-sm btn-secondary"
                                  onClick={() => handleCancelOrder(order.id)}
                                  style={{ color: '#ef4444', padding: '6px 12px' }}
                                >
                                  Hủy đơn
                                </button>
                              </div>
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
                    style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', minWidth: '220px', outline: 'none' }}
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
                  <select
                    value={productStatusFilter}
                    onChange={(e) => setProductStatusFilter(e.target.value)}
                    style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', outline: 'none' }}
                  >
                    <option value="ALL">Tất cả trạng thái</option>
                    <option value="ACTIVE">Đang bán</option>
                    <option value="HIDDEN">Tạm ẩn</option>
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
                      .filter((p) => {
                        const isAct = (p.isAvailable ?? p.status) !== false;
                        if (productStatusFilter === 'ACTIVE') return isAct;
                        if (productStatusFilter === 'HIDDEN') return !isAct;
                        return true;
                      })
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
                            <span
                              style={{
                                padding: '3px 10px',
                                borderRadius: '6px',
                                fontSize: '0.85rem',
                                fontWeight: 700,
                                background: (prod.stock ?? 0) <= 0 ? '#fee2e2' : '#f0fdf4',
                                color: (prod.stock ?? 0) <= 0 ? '#dc2626' : '#15803d',
                              }}
                            >
                              {(prod.stock ?? 0) <= 0 ? '0 (Hết hàng)' : prod.stock}
                            </span>
                          </td>
                          <td style={{ padding: '12px 16px' }}>
                            <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '0.8rem', background: (prod.isAvailable ?? prod.status) ? '#f0fdf4' : '#fef2f2', color: (prod.isAvailable ?? prod.status) ? '#166534' : '#991b1b' }}>
                              {(prod.isAvailable ?? prod.status) ? 'Đang bán' : 'Tạm ẩn'}
                            </span>
                          </td>
                          <td style={{ padding: '12px 16px', textAlign: 'right', whiteSpace: 'nowrap' }}>
                            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                              <button
                                type="button"
                                className="btn btn-sm btn-secondary"
                                onClick={() =>
                                  setEditingProduct({
                                    id: prod.id,
                                    name: prod.name,
                                    categoryId: prod.categoryId || prod.category?.id || safeCategories[0]?.id || 1,
                                    unitPrice: prod.unitPrice || prod.basePrice || 0,
                                    status: prod.status ?? prod.isAvailable ?? true,
                                    imgURL: prod.imgURL || '',
                                  })
                                }
                                style={{ padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem' }}
                              >
                                Sửa
                              </button>
                              <button
                                type="button"
                                className="btn btn-sm btn-outline"
                                onClick={() => handleToggleProduct(prod)}
                                style={{ padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem' }}
                              >
                                {(prod.isAvailable ?? prod.status) ? 'Tạm ẩn' : 'Bật bán'}
                              </button>
                            </div>
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
                          data-error-required="Vui lòng nhập tên món mới"
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
                          min="1"
                          data-error-required="Vui lòng nhập giá bán món mới"
                          data-error-min="Giá bán phải lớn hơn 0"
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

              {/* Modal Chỉnh Sửa Sản Phẩm */}
              {editingProduct && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <div style={{ background: '#fff', borderRadius: '12px', padding: '24px', width: '420px', maxWidth: '90%', border: '1px solid var(--color-border)' }}>
                    <h3 style={{ fontSize: '1.2rem', marginBottom: '16px' }}>Chỉnh Sửa Sản Phẩm</h3>
                    <form onSubmit={handleSaveProductEdit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Tên sản phẩm *</label>
                        <input
                          type="text"
                          required
                          data-error-required="Vui lòng nhập tên sản phẩm"
                          value={editingProduct.name}
                          onChange={(e) => setEditingProduct({ ...editingProduct, name: e.target.value })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                        />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Danh mục *</label>
                        <select
                          value={editingProduct.categoryId}
                          onChange={(e) => setEditingProduct({ ...editingProduct, categoryId: Number(e.target.value) })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                        >
                          {safeCategories.map((c) => (
                            <option key={c.id} value={c.id}>{c.name}</option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '4px' }}>Giá bán (VNĐ) *</label>
                        <input
                          type="number"
                          required
                          min="1"
                          data-error-required="Vui lòng nhập giá bán sản phẩm"
                          data-error-min="Giá bán phải lớn hơn 0"
                          value={editingProduct.unitPrice}
                          onChange={(e) => setEditingProduct({ ...editingProduct, unitPrice: e.target.value })}
                          style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid var(--color-border)' }}
                        />
                      </div>
                      <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '8px' }}>Lưu thay đổi</button>
                        <button type="button" className="btn btn-secondary" onClick={() => setEditingProduct(null)} style={{ flex: 1, padding: '8px' }}>Hủy</button>
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
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', flex: 1 }}>
                  <input
                    type="text"
                    placeholder="Tìm kiếm tên Topping..."
                    value={toppingSearch}
                    onChange={(e) => setToppingSearch(e.target.value)}
                    style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', width: '280px', outline: 'none' }}
                  />
                  <select
                    value={toppingStatusFilter}
                    onChange={(e) => setToppingStatusFilter(e.target.value)}
                    style={{ padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', outline: 'none' }}
                  >
                    <option value="ALL">Tất cả trạng thái</option>
                    <option value="ACTIVE">Đang hoạt động</option>
                    <option value="HIDDEN">Đang tạm ẩn</option>
                  </select>
                </div>
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
                        .filter((t) => {
                          const isAct = t.status !== false;
                          if (toppingStatusFilter === 'ACTIVE') return isAct;
                          if (toppingStatusFilter === 'HIDDEN') return !isAct;
                          return true;
                        })
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
                          data-error-required="Vui lòng nhập tên Topping"
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
                          min="1"
                          data-error-required="Vui lòng nhập đơn giá bán Topping"
                          data-error-min="Đơn giá Topping phải lớn hơn 0"
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
                          data-error-required="Vui lòng nhập số lượng nhập thêm"
                          data-error-min="Số lượng nhập thêm tối thiểu là 1"
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
                          data-error-required="Vui lòng nhập số lượng đưa ra quầy bán"
                          data-error-min="Số lượng đưa ra quầy bán tối thiểu là 1"
                          data-error-max={`Số lượng đưa ra quầy bán tối đa là ${selectedInventoryItem.warehouseStock || 1}`}
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
                          data-error-required="Vui lòng chọn một mặt hàng trong danh sách"
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
                          data-error-required="Vui lòng nhập số lượng nhập thêm"
                          data-error-min="Số lượng nhập thêm tối thiểu là 1"
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
                                    onClick={() => setPromotionToDelete(promo)}
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
                          data-error-required="Vui lòng nhập mã khuyến mãi (Voucher Code)"
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
                            data-error-required="Vui lòng nhập giá trị giảm"
                            data-error-min="Giá trị giảm tối thiểu là 1"
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

              {promotionToDelete && (
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
                  onClick={() => !deletingPromotion && setPromotionToDelete(null)}
                >
                  <div
                    style={{
                      background: '#fff',
                      borderRadius: '16px',
                      padding: '28px 24px',
                      maxWidth: '440px',
                      width: '100%',
                      textAlign: 'center',
                      boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
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
                        margin: '0 auto 16px',
                      }}
                    >
                      <IconAlert size={28} color="#dc2626" />
                    </div>

                    <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 8px', color: '#1e293b' }}>
                      Xác nhận xóa mã khuyến mãi?
                    </h3>

                    <p style={{ color: '#64748b', fontSize: '0.92rem', margin: '0 0 16px', lineHeight: 1.5 }}>
                      Bạn có chắc chắn muốn xóa vĩnh viễn mã khuyến mãi{' '}
                      <strong style={{ color: '#dc2626', background: '#fee2e2', padding: '2px 8px', borderRadius: '4px' }}>
                        {promotionToDelete.code}
                      </strong>
                      ? Thao tác này không thể hoàn tác.
                    </p>

                    <div
                      style={{
                        background: '#f8fafc',
                        border: '1px solid var(--color-border)',
                        borderRadius: '8px',
                        padding: '12px 16px',
                        textAlign: 'left',
                        marginBottom: '20px',
                        fontSize: '0.86rem',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '6px',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#64748b' }}>Mô tả:</span>
                        <strong style={{ color: '#334155' }}>{promotionToDelete.description || 'Không có mô tả'}</strong>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#64748b' }}>Mức giảm:</span>
                        <strong style={{ color: 'var(--color-primary)' }}>
                          {promotionToDelete.discountType === 'PERCENT'
                            ? `Giảm ${promotionToDelete.discountValue}%`
                            : `Giảm ${formatService.formatCurrency(promotionToDelete.discountValue)}`}
                        </strong>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#64748b' }}>Lượt đã dùng:</span>
                        <strong style={{ color: '#334155' }}>
                          {promotionToDelete.usedCount || 0}
                          {promotionToDelete.usageLimit ? ` / ${promotionToDelete.usageLimit} lượt` : ' / Không giới hạn'}
                        </strong>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '12px' }}>
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => setPromotionToDelete(null)}
                        disabled={deletingPromotion}
                        style={{ flex: 1, padding: '10px' }}
                      >
                        Hủy
                      </button>
                      <button
                        type="button"
                        onClick={handleConfirmDeletePromotion}
                        disabled={deletingPromotion}
                        style={{
                          flex: 1,
                          padding: '10px',
                          borderRadius: '8px',
                          background: '#dc2626',
                          color: '#fff',
                          border: 'none',
                          fontWeight: 700,
                          cursor: deletingPromotion ? 'not-allowed' : 'pointer',
                          opacity: deletingPromotion ? 0.7 : 1,
                        }}
                      >
                        {deletingPromotion ? 'Đang xóa...' : 'Xóa vĩnh viễn'}
                      </button>
                    </div>
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
                  required
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
                      <th style={{ padding: '12px 16px' }}>Thao Tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeCategories.map((c, i) => (
                      <tr key={c.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                        <td style={{ padding: '12px 16px', color: '#64748b' }}>#{c.displayOrder || i + 1}</td>
                        <td style={{ padding: '12px 16px', fontWeight: 600 }}>{c.name}</td>
                        <td style={{ padding: '12px 16px' }}>
                          <span style={{
                            padding: '4px 10px',
                            borderRadius: '6px',
                            fontSize: '0.8rem',
                            fontWeight: 500,
                            whiteSpace: 'nowrap',
                            display: 'inline-block',
                            background: c.status !== false ? '#f0fdf4' : '#fef2f2',
                            color: c.status !== false ? '#166534' : '#991b1b'
                          }}>
                            {c.status !== false ? 'Đang hiển thị' : 'Đang tạm ẩn'}
                          </span>
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              type="button"
                              className="btn btn-sm btn-outline"
                              onClick={() => handleToggleCategoryStatus(c)}
                              style={{ padding: '4px 12px', borderRadius: '6px', fontSize: '0.8rem' }}
                            >
                              {c.status !== false ? 'Tạm ẩn' : 'Hiện'}
                            </button>
                            <button
                              type="button"
                              onClick={() => handleOpenDeleteCategory(c)}
                              style={{
                                padding: '4px 12px',
                                borderRadius: '6px',
                                fontSize: '0.8rem',
                                fontWeight: 500,
                                border: '1px solid #fecaca',
                                background: '#fef2f2',
                                color: '#dc2626',
                                cursor: 'pointer',
                              }}
                            >
                              Xóa
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* MODAL POPUP XÁC NHẬN XÓA DANH MỤC */}
              {categoryToDelete && (
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
                  onClick={() => setCategoryToDelete(null)}
                >
                  <div
                    style={{
                      background: '#fff',
                      borderRadius: '16px',
                      padding: '24px',
                      maxWidth: '460px',
                      width: '100%',
                      boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <h3 style={{ margin: '0 0 12px', fontSize: '1.2rem', color: '#1e293b' }}>
                      Xác nhận xóa danh mục
                    </h3>

                    {productsInCategory.length > 0 ? (
                      <div>
                        <p style={{ color: '#dc2626', fontWeight: 600, margin: '0 0 8px' }}>
                          Danh mục "{categoryToDelete.name}" vẫn còn các sản phẩm:
                        </p>
                        <div style={{ maxHeight: '160px', overflowY: 'auto', background: '#f8fafc', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--color-border)', marginBottom: '14px' }}>
                          {productsInCategory.map((prod) => (
                            <div key={prod.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', fontSize: '0.85rem' }}>
                              <span style={{ fontWeight: 500 }}>• {prod.name}</span>
                              <span style={{ color: '#64748b' }}>Số lượng: <strong>{prod.stock ?? 0}</strong></span>
                            </div>
                          ))}
                        </div>
                        <p style={{ fontSize: '0.9rem', color: '#475569', margin: '0 0 16px' }}>
                          Bạn có chắc chắn muốn xóa không? <em>(Lưu ý: Backend sẽ chặn xóa nếu còn sản phẩm, bạn nên ẩn danh mục hoặc chuyển sản phẩm sang danh mục khác).</em>
                        </p>
                      </div>
                    ) : (
                      <p style={{ color: '#475569', fontSize: '0.95rem', margin: '0 0 20px' }}>
                        Bạn có chắc chắn muốn xóa danh mục <strong>"{categoryToDelete.name}"</strong>? Thao tác này không thể hoàn tác.
                      </p>
                    )}

                    <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => setCategoryToDelete(null)}
                        style={{ padding: '8px 18px', borderRadius: '8px' }}
                      >
                        Hủy
                      </button>
                      <button
                        type="button"
                        onClick={handleConfirmDeleteCategory}
                        style={{
                          padding: '8px 18px',
                          borderRadius: '8px',
                          background: '#dc2626',
                          color: '#fff',
                          border: 'none',
                          cursor: 'pointer',
                          fontWeight: 500,
                        }}
                      >
                        {productsInCategory.length > 0 ? 'Vẫn muốn xóa' : 'Xác nhận xóa'}
                      </button>
                    </div>
                  </div>
                </div>
              )}
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
                              {u.role === 'ADMIN' ? (
                                <span style={{ color: '#94a3b8', fontSize: '0.8rem', fontStyle: 'italic' }}>Không khả dụng</span>
                              ) : u.status ? (
                                <button
                                  type="button"
                                  onClick={() => setUserToLock(u)}
                                  style={{
                                    padding: '4px 12px',
                                    borderRadius: '6px',
                                    fontSize: '0.8rem',
                                    fontWeight: 500,
                                    border: '1px solid #fecaca',
                                    background: '#fef2f2',
                                    color: '#dc2626',
                                    cursor: 'pointer',
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
                        aria-label="Đóng"
                        style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                      >
                        <IconClose size={20} />
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
                            data-error-required="Vui lòng nhập tên đăng nhập (Username)"
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
                            data-error-required="Vui lòng nhập mật khẩu khởi tạo"
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
                            data-error-required="Vui lòng nhập họ và tên"
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
                            data-error-required="Vui lòng nhập địa chỉ email"
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
