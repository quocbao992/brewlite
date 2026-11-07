import React, { useState, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import InteractiveButton from '../components/common/InteractiveButton';

export const RegisterPage = ({ onNavigate }) => {
  const { register } = useAuth();
  const [step, setStep] = useState(1);

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    dobDay: '',
    dobMonth: '',
    dobYear: '',
    gender: true,
    username: '',
    password: '',
    confirmPassword: '',
  });

  const dayInputRef = useRef(null);
  const monthInputRef = useRef(null);
  const yearInputRef = useRef(null);

  const [status, setStatus] = useState('idle'); 
  const [errorMsg, setErrorMsg] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'gender' ? value === 'true' : value,
    }));
  };

  const isLeapYear = (y) => {
    return (y % 4 === 0 && y % 100 !== 0) || (y % 400 === 0);
  };

  const validateDateOfBirth = (dStr, mStr, yStr) => {
    if (!dStr || !dStr.trim()) return 'Vui lòng nhập ngày sinh.';
    if (!mStr || !mStr.trim()) return 'Vui lòng nhập tháng sinh.';
    if (!yStr || !yStr.trim()) return 'Vui lòng nhập năm sinh.';

    const day = parseInt(dStr, 10);
    const month = parseInt(mStr, 10);
    const year = parseInt(yStr, 10);

    if (isNaN(day) || isNaN(month) || isNaN(year)) {
      return 'Ngày, tháng, năm sinh chỉ được chứa chữ số.';
    }

    if (month < 1 || month > 12) {
      return 'Tháng sinh không hợp lệ (từ 01 đến 12).';
    }

    const currentYear = new Date().getFullYear();
    const minYear = currentYear - 100; 
    const maxYear = currentYear - 10;  

    if (year < minYear || year > maxYear) {
      return `Độ tuổi hợp lệ là từ 10 đến 100 tuổi (năm sinh từ ${minYear} đến ${maxYear}).`;
    }

    if (day < 1) {
      return 'Ngày sinh phải từ ngày 01 trở lên.';
    }

    if (month === 2) {
      const isLeap = isLeapYear(year);
      const maxFebDays = isLeap ? 29 : 28;
      if (day > maxFebDays) {
        return isLeap
          ? `Năm ${year} là năm nhuận nên tháng 2 chỉ có tối đa 29 ngày.`
          : `Năm ${year} không phải năm nhuận nên tháng 2 chỉ có tối đa 28 ngày.`;
      }
    } else if ([4, 6, 9, 11].includes(month)) {
      if (day > 30) {
        return `Tháng ${month} chỉ có tối đa 30 ngày.`;
      }
    } else {
      if (day > 31) {
        return `Tháng ${month} chỉ có tối đa 31 ngày.`;
      }
    }

    const birthDate = new Date(year, month - 1, day);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }

    if (age < 10) {
      return 'Khách hàng phải từ đủ 10 tuổi trở lên để đăng ký tài khoản.';
    }
    if (age > 100) {
      return 'Năm sinh không hợp lệ (vượt quá 100 tuổi).';
    }

    return null;
  };

  const handleDayChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 2);
    setFormData((prev) => ({ ...prev, dobDay: val }));
    if (val.length === 2 && monthInputRef.current) {
      monthInputRef.current.focus();
    }
  };

  const handleMonthChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 2);
    setFormData((prev) => ({ ...prev, dobMonth: val }));
    if (val.length === 2 && yearInputRef.current) {
      yearInputRef.current.focus();
    }
  };

  const handleYearChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 4);
    setFormData((prev) => ({ ...prev, dobYear: val }));
  };

  const handleKeyDown = (field, e) => {
    if (e.key === 'Backspace') {
      if (field === 'month' && !formData.dobMonth && dayInputRef.current) {
        dayInputRef.current.focus();
      } else if (field === 'year' && !formData.dobYear && monthInputRef.current) {
        monthInputRef.current.focus();
      }
    }
  };

  const handleNextStep = (e) => {
    e.preventDefault();
    setErrorMsg('');

    const nameVal = formData.fullName.trim();
    if (!nameVal) {
      setErrorMsg('Vui lòng nhập họ và tên.');
      return;
    }
    // Kiểm tra họ tên: chỉ chấp nhận chữ cái tiếng Việt/quốc tế và dấu cách, cấm chữ số và ký tự đặc biệt
    const nameRegex = /^[\p{L}\s]+$/u;
    if (!nameRegex.test(nameVal) || nameVal.length < 2) {
      setErrorMsg('Họ và tên không hợp lệ. Chỉ được chứa chữ cái, không được chứa số (ví dụ: 888) hoặc ký tự đặc biệt.');
      return;
    }
    if (!formData.email.trim() || !formData.email.includes('@')) {
      setErrorMsg('Email không hợp lệ.');
      return;
    }
    if (!/^(0|\+84)[0-9]{9}$/.test(formData.phone.trim())) {
      setErrorMsg('Số điện thoại không hợp lệ (gồm 10 chữ số).');
      return;
    }

    const dateErr = validateDateOfBirth(formData.dobDay, formData.dobMonth, formData.dobYear);
    if (dateErr) {
      setErrorMsg(dateErr);
      if (!formData.dobDay && dayInputRef.current) dayInputRef.current.focus();
      else if (!formData.dobMonth && monthInputRef.current) monthInputRef.current.focus();
      else if (!formData.dobYear && yearInputRef.current) yearInputRef.current.focus();
      return;
    }

    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!formData.username.trim() || formData.username.trim().length < 3) {
      setErrorMsg('Tên tài khoản phải từ 3 ký tự trở lên.');
      return;
    }
    if (!formData.password || formData.password.length < 8) {
      setErrorMsg('Mật khẩu phải từ 8 ký tự trở lên.');
      return;
    }
    if (formData.password !== formData.confirmPassword) {
      setErrorMsg('Mật khẩu xác nhận không khớp.');
      return;
    }

    setStatus('loading');

    const formattedDob = `${formData.dobYear}-${formData.dobMonth.padStart(2, '0')}-${formData.dobDay.padStart(2, '0')}`;

    const payload = {
      username: formData.username.trim(),
      password: formData.password,
      fullName: formData.fullName.trim(),
      email: formData.email.trim().toLowerCase(),
      phone: formData.phone.trim(),
      dateOfBirth: formattedDob,
      gender: Boolean(formData.gender),
    };

    const result = await register(payload);

    if (result.success) {
      setStatus('success');
    } else {
      setErrorMsg(result.error || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.');
      setStatus('error');
    }
  };

  const handleReset = () => {
    setStatus('idle');
    setErrorMsg('');
    setStep(1);
    setFormData({
      fullName: '',
      email: '',
      phone: '',
      dobDay: '',
      dobMonth: '',
      dobYear: '',
      gender: true,
      username: '',
      password: '',
      confirmPassword: '',
    });
  };

  return (
    <div
      className="container"
      style={{
        padding: '50px 24px',
        maxWidth: '520px',
        margin: '0 auto',
      }}
    >
      <div
        style={{
          background: 'var(--color-surface)',
          padding: '36px',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-md)',
          border: '1px solid var(--color-border)',
        }}
      >
        {status === 'success' ? (
          <div style={{ textAlign: 'center', padding: '16px 8px' }}>
            <div className="anim-circle-wrap">
              <svg className="anim-svg" viewBox="0 0 100 100">
                <circle className="anim-circle-success" cx="50" cy="50" r="45" />
                <polyline className="anim-check" points="28,52 42,66 72,36" />
              </svg>
            </div>

            <h2
              style={{
                fontSize: '1.8rem',
                fontWeight: 800,
                color: 'var(--color-success)',
                marginBottom: '10px',
              }}
            >
              Đăng Ký Thành Công!
            </h2>
            <p
              style={{
                color: 'var(--color-text-secondary)',
                fontSize: '0.98rem',
                marginBottom: '28px',
                lineHeight: 1.6,
              }}
            >
              Tài khoản <strong>{formData.username}</strong> đã được tạo thành công!<br />
              Bạn có thể đăng nhập ngay để bắt đầu trải nghiệm BrewLite.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <button
                className="btn btn-primary"
                style={{ width: '100%', padding: '12px', fontSize: '1rem', fontWeight: 700 }}
                onClick={() => onNavigate('login')}
              >
                Đăng nhập ngay
              </button>
              <button
                className="btn btn-secondary"
                style={{ width: '100%', padding: '10px', fontSize: '0.92rem' }}
                onClick={() => onNavigate('home')}
              >
                Về trang chủ
              </button>
            </div>
          </div>
        ) : (
          <>
            <div style={{ textAlign: 'center', marginBottom: '24px' }}>
              <h2 style={{ fontSize: '1.8rem', fontWeight: 800 }}>Tạo Tài Khoản Mới</h2>
              <p style={{ color: 'var(--color-text-secondary)', marginTop: '4px' }}>
                Đăng ký để tích điểm và nhận ưu đãi từ BrewLite
              </p>

              {/* Chỉ báo bước (Step indicator) */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  marginTop: '18px',
                  fontSize: '0.85rem',
                  fontWeight: 700,
                }}
              >
                <div
                  style={{
                    padding: '4px 12px',
                    borderRadius: '20px',
                    background: step === 1 ? 'var(--color-primary)' : '#e2e8f0',
                    color: step === 1 ? '#fff' : 'var(--color-text-secondary)',
                    transition: 'all 0.3s ease',
                  }}
                >
                  1. Thông tin cá nhân
                </div>
                <div style={{ width: '16px', height: '2px', background: '#cbd5e1' }} />
                <div
                  style={{
                    padding: '4px 12px',
                    borderRadius: '20px',
                    background: step === 2 ? 'var(--color-primary)' : '#e2e8f0',
                    color: step === 2 ? '#fff' : 'var(--color-text-secondary)',
                    transition: 'all 0.3s ease',
                  }}
                >
                  2. Tài khoản & Mật khẩu
                </div>
              </div>
            </div>

            {errorMsg && (
              <div
                style={{
                  padding: '12px 14px',
                  background: '#fee2e2',
                  color: '#b91c1c',
                  borderRadius: 'var(--radius-md)',
                  marginBottom: '20px',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              >
                {errorMsg}
              </div>
            )}

            {/* Bước 1: Thông tin cá nhân */}
            {step === 1 && (
              <form noValidate onSubmit={handleNextStep}>
                <div className="form-group">
                  <label className="form-label">Họ và tên *</label>
                  <input
                    type="text"
                    name="fullName"
                    className="form-input"
                    placeholder="VD: Nguyễn Văn A"
                    value={formData.fullName}
                    onChange={handleChange}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Email *</label>
                  <input
                    type="email"
                    name="email"
                    className="form-input"
                    placeholder="name@example.com"
                    value={formData.email}
                    onChange={handleChange}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Số điện thoại *</label>
                  <input
                    type="tel"
                    name="phone"
                    className="form-input"
                    placeholder="VD: 0912345678"
                    value={formData.phone}
                    onChange={handleChange}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: '14px', alignItems: 'start' }}>
                  <div className="form-group">
                    <label className="form-label">Ngày sinh *</label>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.15fr 1.25fr', gap: '6px' }}>
                      <input
                        ref={dayInputRef}
                        type="text"
                        inputMode="numeric"
                        maxLength={2}
                        className="form-input"
                        placeholder="Ngày"
                        title="Ngày sinh (01 - 31)"
                        value={formData.dobDay}
                        onChange={handleDayChange}
                        onKeyDown={(e) => handleKeyDown('day', e)}
                        style={{ textAlign: 'center', padding: '10px 4px', fontSize: '0.92rem' }}
                      />
                      <input
                        ref={monthInputRef}
                        type="text"
                        list="dob-months-options"
                        className="form-input"
                        placeholder="Tháng"
                        title="Lướt chọn hoặc gõ tháng (01 - 12)"
                        value={formData.dobMonth}
                        onChange={handleMonthChange}
                        onKeyDown={(e) => handleKeyDown('month', e)}
                        style={{ textAlign: 'center', padding: '10px 4px', fontSize: '0.92rem' }}
                      />
                      <datalist id="dob-months-options">
                        {Array.from({ length: 12 }, (_, i) => {
                          const val = String(i + 1).padStart(2, '0');
                          return <option key={val} value={val}>{`Tháng ${val}`}</option>;
                        })}
                      </datalist>
                      <input
                        ref={yearInputRef}
                        type="text"
                        inputMode="numeric"
                        maxLength={4}
                        className="form-input"
                        placeholder="Năm"
                        title="Năm sinh (4 chữ số)"
                        value={formData.dobYear}
                        onChange={handleYearChange}
                        onKeyDown={(e) => handleKeyDown('year', e)}
                        style={{ textAlign: 'center', padding: '10px 4px', fontSize: '0.92rem' }}
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Giới tính *</label>
                    <select
                      name="gender"
                      className="form-select"
                      value={formData.gender ? 'true' : 'false'}
                      onChange={handleChange}
                      style={{ padding: '10px 12px' }}
                    >
                      <option value="true">Nam</option>
                      <option value="false">Nữ</option>
                    </select>
                  </div>
                </div>

                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ width: '100%', padding: '12px', marginTop: '16px' }}
                >
                  Tiếp tục
                </button>
              </form>
            )}

            {/* Bước 2: Thông tin tài khoản */}
            {step === 2 && (
              <form noValidate onSubmit={handleSubmit}>
                <div className="form-group">
                  <label className="form-label">Tên tài khoản (Username) *</label>
                  <input
                    type="text"
                    name="username"
                    className="form-input"
                    placeholder="Tên đăng nhập (từ 3 ký tự)"
                    value={formData.username}
                    onChange={handleChange}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Mật khẩu *</label>
                  <input
                    type="password"
                    name="password"
                    className="form-input"
                    placeholder="Mật khẩu (tối thiểu 8 ký tự)"
                    value={formData.password}
                    onChange={handleChange}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Xác nhận mật khẩu *</label>
                  <input
                    type="password"
                    name="confirmPassword"
                    className="form-input"
                    placeholder="Nhập lại mật khẩu"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                  />
                </div>

                <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ flex: 1, padding: '12px' }}
                    onClick={() => setStep(1)}
                  >
                    Quay lại
                  </button>

                  <div style={{ flex: 2 }}>
                    <InteractiveButton
                      text="Xác nhận đăng ký"
                      loadingText="Đang tạo tài khoản..."
                      successText="Đăng ký thành công!"
                      loading={status === 'loading'}
                      success={status === 'success'}
                      type="submit"
                      style={{ padding: '12px' }}
                    />
                  </div>
                </div>
              </form>
            )}

            <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.92rem' }}>
              Đã có tài khoản?{' '}
              <button
                onClick={() => onNavigate('login')}
                style={{ color: 'var(--color-accent)', fontWeight: 700 }}
              >
                Đăng nhập tại đây
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default RegisterPage;
