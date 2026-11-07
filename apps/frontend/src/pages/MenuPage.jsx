import React, { useEffect, useState, useRef } from 'react';
import { productApi, categoryApi } from '../api';
import ProductCard from '../components/common/ProductCard';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ChiikawaMascot from '../components/common/ChiikawaMascot';
import { formatService } from '../services/formatService';

export const MenuPage = ({ onNavigate }) => {
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [allProducts, setAllProducts] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const searchWrapRef = useRef(null);

  useEffect(() => {
    loadCategories();
    loadAllProductsForSuggestions();
  }, []);

  useEffect(() => {
    loadProducts();
  }, [selectedCategory, currentPage]);

  // Đóng gợi ý khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (searchWrapRef.current && !searchWrapRef.current.contains(e.target)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const loadCategories = async () => {
    try {
      const res = await categoryApi.getAll();
      const list = Array.isArray(res) ? res : res?.data || [];
      setCategories(list);
    } catch (err) {
      console.error('Lỗi khi tải danh mục từ cơ sở dữ liệu:', err);
      setCategories([]);
    }
  };

  const loadAllProductsForSuggestions = async () => {
    try {
      const res = await productApi.search({ isAvailable: true, size: 50 });
      const items = res?.content || (Array.isArray(res) ? res : []);
      setAllProducts(items);
    } catch (err) {
      console.warn('Lỗi khi tải danh sách gợi ý:', err);
    }
  };

  const loadProducts = async (overrideKeyword) => {
    try {
      setLoading(true);
      const searchKey = overrideKeyword !== undefined ? overrideKeyword : keyword;
      const res = await productApi.search({
        categoryId: selectedCategory || undefined,
        keyword: searchKey.trim() || undefined,
        isAvailable: true,
        page: currentPage,
        size: 8,
      });

      const items = res?.content || (Array.isArray(res) ? res : []);
      setProducts(items);
      setTotalPages(res?.totalPages || 1);
    } catch (err) {
      console.error('Lỗi khi tải sản phẩm từ cơ sở dữ liệu:', err);
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e) => {
    if (e) e.preventDefault();
    setShowSuggestions(false);
    setCurrentPage(0);
    loadProducts();
  };

  const handleSelectSuggestion = (productName) => {
    setKeyword(productName);
    setShowSuggestions(false);
    setCurrentPage(0);
    loadProducts(productName);
  };

  // Lọc danh sách gợi ý khi người dùng bắt đầu gõ chữ (>= 1 ký tự)
  const suggestions = keyword.trim().length >= 1
    ? allProducts.filter((p) =>
        (p.name || '').toLowerCase().includes(keyword.trim().toLowerCase())
      ).slice(0, 5)
    : [];

  return (
    <div className="container" style={{ padding: '40px 24px 60px' }}>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '20px',
          marginBottom: '28px',
        }}
      >
        <div>
          <h1 style={{ fontSize: '2.4rem', fontWeight: 800 }}>Thực Đơn Đồ Uống</h1>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: '4px' }}>
            Khám phá hương vị đậm đà được pha chế từ nguyên liệu tươi nguyên
          </p>
        </div>

        {/* Ô tìm kiếm có gợi ý trực tiếp khi gõ chữ */}
        <div ref={searchWrapRef} style={{ position: 'relative', minWidth: '340px', flex: '1', maxWidth: '420px' }}>
          <form
            onSubmit={handleSearchSubmit}
            style={{ display: 'flex', gap: '8px', width: '100%' }}
          >
            <input
              type="text"
              className="form-input"
              placeholder="Tìm món (VD: cà phê, trà...)"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setShowSuggestions(true);
              }}
              onFocus={() => {
                if (keyword.trim().length >= 1) setShowSuggestions(true);
              }}
              style={{ flex: 1 }}
            />
            <button type="submit" className="btn btn-primary btn-sm" style={{ whiteSpace: 'nowrap', padding: '0 16px' }}>
              Tìm kiếm
            </button>
          </form>

          {/* Hộp gợi ý tìm kiếm (Dropdown suggestions) */}
          {showSuggestions && suggestions.length > 0 && (
            <div
              style={{
                position: 'absolute',
                top: 'calc(100% + 6px)',
                left: 0,
                right: 0,
                background: 'var(--color-surface, #ffffff)',
                borderRadius: 'var(--radius-md, 8px)',
                boxShadow: '0 8px 24px rgba(0, 0, 0, 0.15)',
                border: '1px solid var(--color-border, #e2e8f0)',
                zIndex: 100,
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  padding: '8px 12px',
                  background: 'var(--color-surface-muted, #f8fafc)',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  color: 'var(--color-text-secondary, #64748b)',
                  borderBottom: '1px solid var(--color-border, #e2e8f0)',
                }}
              >
                Gợi ý món phù hợp:
              </div>
              {suggestions.map((item) => (
                <div
                  key={item.id}
                  onClick={() => handleSelectSuggestion(item.name)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    cursor: 'pointer',
                    borderBottom: '1px solid #f1f5f9',
                    transition: 'background 0.2s ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.background = '#f8fafc')}
                  onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.92rem', color: 'var(--color-text-main, #1e293b)' }}>
                      {item.name}
                    </div>
                    {item.categoryName && (
                      <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary, #64748b)' }}>
                        {item.categoryName}
                      </span>
                    )}
                  </div>
                  <div style={{ fontWeight: 700, fontSize: '0.88rem', color: 'var(--color-accent, #c2410c)' }}>
                    {formatService.formatMoney(item.unitPrice)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Bé mascot Chiikawa hướng dẫn khách hàng */}
      <ChiikawaMascot
        character="hachiware"
        message="Bạn có thể chọn topping và size khi ấn đặt món"
      />

      <div className="category-tabs">
        <button
          className={`tab-btn ${selectedCategory === null ? 'active' : ''}`}
          onClick={() => {
            setSelectedCategory(null);
            setCurrentPage(0);
          }}
        >
          Tất cả món
        </button>
        {categories.map((cat) => (
          <button
            key={cat.id}
            className={`tab-btn ${selectedCategory === cat.id ? 'active' : ''}`}
            onClick={() => {
              setSelectedCategory(cat.id);
              setCurrentPage(0);
            }}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {loading ? (
        <LoadingSpinner text="Đang tải danh sách đồ uống..." />
      ) : products.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <p style={{ fontSize: '1.2rem', color: 'var(--color-text-secondary)' }}>
            Không tìm thấy sản phẩm nào phù hợp.
          </p>
        </div>
      ) : (
        <>
          <div className="product-grid">
            {products.map((p) => (
              <ProductCard key={p.id} product={p} onNavigate={onNavigate} />
            ))}
          </div>

          {totalPages > 1 && (
            <div
              style={{
                display: 'flex',
                justifyContent: 'center',
                gap: '8px',
                marginTop: '20px',
              }}
            >
              <button
                className="btn btn-secondary btn-sm"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
              >
                Trước
              </button>
              <span
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  padding: '0 12px',
                  fontWeight: 700,
                }}
              >
                Trang {currentPage + 1} / {totalPages}
              </span>
              <button
                className="btn btn-secondary btn-sm"
                disabled={currentPage >= totalPages - 1}
                onClick={() => setCurrentPage((prev) => prev + 1)}
              >
                Sau
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default MenuPage;
