import React, { useEffect, useState } from 'react';
import { productApi } from '../api';
import ProductCard from '../components/common/ProductCard';
import LoadingSpinner from '../components/common/LoadingSpinner';
import WeatherWidget from '../components/common/WeatherWidget';
import ChiikawaMascot from '../components/common/ChiikawaMascot';
import { localImages } from '../images';

export const HomePage = ({ onNavigate }) => {
  const [featuredProducts, setFeaturedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [suggestedDrink, setSuggestedDrink] = useState('Cà phê sữa đá');

  const handleWeatherChange = (weatherData) => {
    if (!weatherData) return;
    const temp = weatherData.temperature ?? 27;
    const desc = weatherData.description || '';

    if (desc.includes('Mưa')) {
      setSuggestedDrink('Cà phê Latte nóng thơm béo');
    } else if (temp >= 30) {
      setSuggestedDrink('Cold Brew cam sả mát lạnh');
    } else if (temp <= 22) {
      setSuggestedDrink('Bạc xỉu nóng 3 tầng');
    } else {
      setSuggestedDrink('Cà phê sữa đá đậm vị');
    }
  };

  useEffect(() => {
    loadFeatured();
  }, []);

  const loadFeatured = async () => {
    try {
      setLoading(true);
      const res = await productApi.getFeatured();
      const list = Array.isArray(res) ? res : res?.data || res?.content || [];
      if (list && list.length > 0) {
        setFeaturedProducts(list);
      } else {
        const fallbackRes = await productApi.search({ isAvailable: true, size: 8 });
        const fallbackList = fallbackRes?.content || (Array.isArray(fallbackRes) ? fallbackRes : []);
        setFeaturedProducts(fallbackList);
      }
    } catch (err) {
      console.error('Lỗi khi tải món nổi bật từ CSDL:', err);
      setFeaturedProducts([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <section className="hero">
        <div className="container hero-grid">
          <div>
            <h1 className="hero-title">
              Khởi đầu ngày mới với một ly <span>"{suggestedDrink}"</span> để cho ngày mới tỉnh táo, tràn trề năng lượng.
            </h1>
            <div className="hero-cta">
              <button
                className="btn btn-primary"
                onClick={() => onNavigate('menu')}
              >
                Khám phá thực đơn
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => onNavigate('cart')}
              >
                Giỏ hàng của bạn
              </button>
            </div>
          </div>

          <div>
            <WeatherWidget onWeatherChange={handleWeatherChange} />
          </div>
        </div>
      </section>

      <section className="container" style={{ padding: '40px 24px 60px' }}>
        {/* Bé mascot Chiikawa hướng dẫn đặt món */}
        <ChiikawaMascot
          character="chiikawa"
          message="Bạn có thể chọn topping và size khi ấn đặt món"
          actionText="Khám phá thực đơn"
          onActionClick={() => onNavigate('menu')}
        />

        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            marginBottom: '32px',
          }}
        >
          <div>
            <span
              style={{
                fontSize: '0.85rem',
                fontWeight: 700,
                color: 'var(--color-accent)',
                textTransform: 'uppercase',
              }}
            >
              Món Đang Được Yêu Thích
            </span>
            <h2 style={{ fontSize: '2rem', fontWeight: 800, marginTop: '4px' }}>
              Món nổi bật hôm nay
            </h2>
          </div>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => onNavigate('menu')}
          >
            Xem tất cả món
          </button>
        </div>


        {loading ? (
          <LoadingSpinner text="Đang tải món nổi bật..." />
        ) : (
          <div className="product-grid">
            {featuredProducts.slice(0, 4).map((p) => (
              <ProductCard key={p.id} product={p} onNavigate={onNavigate} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export default HomePage;
