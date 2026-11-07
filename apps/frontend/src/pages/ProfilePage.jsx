import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { customerApi, userApi } from '../api';
import LoadingSpinner from '../components/common/LoadingSpinner';

export const ProfilePage = ({ onNavigate }) => {
  const { user, setUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [dobDay, setDobDay] = useState('');
  const [dobMonth, setDobMonth] = useState('');
  const [dobYear, setDobYear] = useState('');
  const [gender, setGender] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState('');
  const [profileError, setProfileError] = useState('');

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordError, setPasswordError] = useState('');

  useEffect(() => {
    loadProfile();
  }, [user?.id]);

  const getRoleBadge = (role) => {
    const normalized = String(role || '').toUpperCase().replace(/^ROLE_/, '').trim();
    switch (normalized) {
      case 'ADMIN':
        return { label: 'Quản trị viên (ADMIN)', bg: '#fef3c7', color: '#b45309' };
      case 'BARISTA':
        return { label: 'Nhân viên pha chế (Barista)', bg: '#e0e7ff', color: '#4338ca' };
      case 'CASHIER':
        return { label: 'Thu ngân', bg: '#dcfce7', color: '#15803d' };
      case 'STAFF':
        return { label: 'Nhân viên hệ thống', bg: '#dcfce7', color: '#15803d' };
      case 'CUSTOMER':
        return { label: 'Khách hàng thân thiết', bg: '#e0f2fe', color: '#0369a1' };
      default:
        return { label: normalized || 'Khách hàng thân thiết', bg: '#e0f2fe', color: '#0369a1' };
    }
  };

  const loadProfile = async () => {
    if (!user?.id) {
      setLoading(false);
      return;
    }
    setProfileError('');
    try {
      setLoading(true);
      let data = null;
      if (user.role === 'CUSTOMER') {
        try {
          data = await customerApi.getProfile(user.id);
        } catch {
          data = await userApi.getProfile(user.id).catch(() => null);
        }
      } else {
        data = await userApi.getProfile(user.id).catch(() => null);
      }

      if (data) {
        setProfile(data);
        setFullName(data.fullName || user.fullName || user.username || '');
        setPhone(data.phone || user.phone || '');
        if (data.dateOfBirth) {
          const parts = String(data.dateOfBirth).split(/[/.-]/);
          if (parts.length === 3) {
            if (parts[0].length === 4) {
              setDobYear(parts[0]);
              setDobMonth(parts[1]);
              setDobDay(parts[2]);
            } else {
              setDobDay(parts[0]);
              setDobMonth(parts[1]);
              setDobYear(parts[2]);
            }
          }
        }
        setGender(data.gender !== undefined && data.gender !== null ? Boolean(data.gender) : true);
      } else {
        setProfile({
          username: user.username,
          fullName: user.fullName || user.username,
          email: user.email,
          role: user.role,
        });
        setFullName(user.fullName || user.username || '');
        setPhone(user.phone || '');
      }
    } catch (err) {
      console.warn('Lỗi khi tải thông tin cá nhân:', err);
      setProfile({
        username: user?.username,
        fullName: user?.fullName || user?.username,
        email: user?.email,
        role: user?.role,
      });
      setFullName(user?.fullName || user?.username || '');
    } finally {
      setLoading(false);
    }
  };

  const isLeapYear = (y) => {
    return (y % 4 === 0 && y % 100 !== 0) || (y % 400 === 0);
  };

  const validateDateOfBirth = (dStr, mStr, yStr) => {
    if (!dStr && !mStr && !yStr) return null;
    if (!dStr || !dStr.trim()) return 'Vui lòng nhập ngày sinh.';
    if (!mStr || !mStr.trim()) return 'Vui lòng nhập tháng sinh.';
    if (!yStr || !yStr.trim()) return 'Vui lòng nhập năm sinh.';

    const day = parseInt(dStr, 10);
    const month = parseInt(mStr, 10);
    const year = parseInt(yStr, 10);

    if (isNaN(day) || isNaN(month) || isNaN(year)) {
      return 'Ngày, tháng, năm sinh phải là chữ số.';
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
      return 'Khách hàng phải từ đủ 10 tuổi trở lên.';
    }
    if (age > 100) {
      return 'Năm sinh không hợp lệ (vượt quá 100 tuổi).';
    }

    return null;
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setProfileSuccess('');
    setProfileError('');

    if (!fullName.trim()) {
      setProfileError('Họ và tên không được để trống.');
      return;
    }
    if (phone.trim() && !/^(0|\+84)[0-9]{9}$/.test(phone.trim())) {
      setProfileError('Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng 0 hoặc +84).');
      return;
    }

    if (dobDay || dobMonth || dobYear) {
      const dateErr = validateDateOfBirth(dobDay, dobMonth, dobYear);
      if (dateErr) {
        setProfileError(dateErr);
        return;
      }
    }

    const isoDob = (dobDay && dobMonth && dobYear)
      ? `${dobYear}-${dobMonth.padStart(2, '0')}-${dobDay.padStart(2, '0')}`
      : null;

    try {
      setSavingProfile(true);
      const payload = {
        fullName: fullName.trim(),
        phone: phone.trim(),
        dateOfBirth: isoDob,
        gender: Boolean(gender),
      };

      let updated = null;
      if (user?.role === 'CUSTOMER') {
        try {
          updated = await customerApi.updateProfile(user.id, payload);
        } catch {
          updated = await userApi.updateProfile(user.id, { fullName: fullName.trim(), phone: phone.trim() });
        }
      } else {
        updated = await userApi.updateProfile(user.id, { fullName: fullName.trim(), phone: phone.trim() });
      }

      setProfile((prev) => ({ ...prev, ...updated, fullName: fullName.trim(), phone: phone.trim() }));
      if (updated && updated.gender !== undefined && updated.gender !== null) {
        setGender(Boolean(updated.gender));
      }
      setProfileSuccess('Cập nhật thông tin thành công!');

      if (setUser && user) {
        setUser({ ...user, fullName: fullName.trim(), phone: phone.trim() });
      }
    } catch (err) {
      setProfileError(err.message || 'Cập nhật thông tin thất bại.');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordSuccess('');
    setPasswordError('');

    if (!oldPassword) {
      setPasswordError('Vui lòng nhập mật khẩu hiện tại.');
      return;
    }
    if (newPassword.length < 8 || newPassword.length > 32) {
      setPasswordError('Mật khẩu mới phải từ 8 đến 32 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Mật khẩu xác nhận không khớp với mật khẩu mới.');
      return;
    }

    try {
      setSavingPassword(true);
      await userApi.changePassword(user.id, {
        oldPassword,
        newPassword,
        confirmPassword,
      });
      setPasswordSuccess('Đổi mật khẩu thành công!');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setPasswordError(err.message || 'Đổi mật khẩu thất bại.');
    } finally {
      setSavingPassword(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ padding: '80px 24px', textAlign: 'center' }}>
        <LoadingSpinner text="Đang tải thông tin khách hàng..." />
      </div>
    );
  }

  const loyaltyPts = profile?.loyaltyPoints ?? profile?.loyaltyPoint ?? 0;
  const rawRole = user?.role || profile?.role || 'CUSTOMER';
  const currentRole = String(rawRole).toUpperCase().replace(/^ROLE_/, '').trim();
  const roleBadge = getRoleBadge(currentRole);

  return (
    <div className="container" style={{ padding: '40px 24px 60px', maxWidth: '880px', margin: '0 auto' }}>
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontSize: '2.2rem', fontWeight: 800 }}>
          {currentRole === 'CUSTOMER' ? 'Thông Tin Khách Hàng' : 'Thông Tin Tài Khoản'}
        </h1>
        <p style={{ color: 'var(--color-text-secondary)', marginTop: '4px' }}>
          {currentRole === 'CUSTOMER'
            ? 'Quản lý hồ sơ cá nhân, điểm tích lũy và bảo mật tài khoản BrewLite'
            : 'Quản lý thông tin quản trị viên, thông tin liên hệ và bảo mật tài khoản'}
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>
        {/* Cột trái: Thẻ tổng quan + Đổi mật khẩu */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Card Tổng quan */}
          <div
            style={{
              background: 'var(--color-surface)',
              padding: '24px',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--color-border)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '50%',
                  background: 'var(--color-primary)',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.4rem',
                  fontWeight: 800,
                  textTransform: 'uppercase',
                }}
              >
                {(profile?.fullName || user?.username || 'U').charAt(0)}
              </div>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 800 }}>
                  {profile?.fullName || user?.fullName || user?.username}
                </h3>
                <span
                  style={{
                    display: 'inline-block',
                    marginTop: '4px',
                    padding: '2px 8px',
                    borderRadius: '12px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    background: roleBadge.bg,
                    color: roleBadge.color,
                  }}
                >
                  {roleBadge.label}
                </span>
              </div>
            </div>

            <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.92rem' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Tên đăng nhập:</span>
                <span style={{ fontWeight: 600 }}>{profile?.username || user?.username}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.92rem' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Email:</span>
                <span style={{ fontWeight: 600 }}>{profile?.email || user?.email || 'Chưa cập nhật'}</span>
              </div>
              {currentRole === 'CUSTOMER' ? (
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.92rem' }}>
                  <span style={{ color: 'var(--color-text-secondary)' }}>Điểm tích lũy:</span>
                  <span style={{ fontWeight: 800, color: 'var(--color-accent)' }}>{loyaltyPts} điểm</span>
                </div>
              ) : (
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.92rem' }}>
                  <span style={{ color: 'var(--color-text-secondary)' }}>Quyền hạn:</span>
                  <span style={{ fontWeight: 700, color: '#16a34a' }}>
                    {currentRole === 'ADMIN' ? 'Toàn quyền quản trị hệ thống' : currentRole}
                  </span>
                </div>
              )}
            </div>

            <div style={{ marginTop: '20px', display: 'flex', gap: '8px' }}>
              <button
                className="btn btn-secondary btn-sm"
                style={{ flex: 1 }}
                onClick={() => onNavigate('orders')}
              >
                Xem đơn hàng
              </button>
              <button
                className="btn btn-outline btn-sm"
                style={{ flex: 1 }}
                onClick={() => onNavigate('menu')}
              >
                Xem thực đơn
              </button>
            </div>
          </div>

          {/* Card Đổi mật khẩu */}
          <div
            style={{
              background: 'var(--color-surface)',
              padding: '24px',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--color-border)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: '16px' }}>
              Đổi Mật Khẩu
            </h3>

            {passwordSuccess && (
              <div
                style={{
                  padding: '10px 14px',
                  background: '#dcfce7',
                  color: '#15803d',
                  borderRadius: 'var(--radius-md)',
                  marginBottom: '16px',
                  fontSize: '0.88rem',
                  fontWeight: 600,
                }}
              >
                {passwordSuccess}
              </div>
            )}

            {passwordError && (
              <div
                style={{
                  padding: '10px 14px',
                  background: '#fee2e2',
                  color: '#b91c1c',
                  borderRadius: 'var(--radius-md)',
                  marginBottom: '16px',
                  fontSize: '0.88rem',
                  fontWeight: 600,
                }}
              >
                {passwordError}
              </div>
            )}

            <form onSubmit={handleChangePassword}>
              <div className="form-group">
                <label className="form-label">Mật khẩu hiện tại</label>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Nhập mật khẩu hiện tại"
                  required
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Mật khẩu mới (tối thiểu 8 ký tự)</label>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Nhập mật khẩu mới"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Xác nhận mật khẩu mới</label>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Nhập lại mật khẩu mới"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>

              <button
                type="submit"
                className="btn btn-secondary"
                style={{ width: '100%', padding: '10px', marginTop: '6px' }}
                disabled={savingPassword}
              >
                {savingPassword ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
              </button>
            </form>
          </div>
        </div>

        {/* Cột phải: Form cập nhật thông tin cá nhân */}
        <div
          style={{
            background: 'var(--color-surface)',
            padding: '28px',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--color-border)',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '20px' }}>
            Chỉnh Sửa Thông Tin Cá Nhân
          </h3>

          {profileSuccess && (
            <div
              style={{
                padding: '12px 14px',
                background: '#dcfce7',
                color: '#15803d',
                borderRadius: 'var(--radius-md)',
                marginBottom: '20px',
                fontSize: '0.9rem',
                fontWeight: 600,
              }}
            >
              {profileSuccess}
            </div>
          )}

          {profileError && (
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
              {profileError}
            </div>
          )}

          <form onSubmit={handleUpdateProfile}>
            <div className="form-group">
              <label className="form-label">Họ và tên</label>
              <input
                type="text"
                className="form-input"
                placeholder="Nhập họ và tên đầy đủ"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Số điện thoại</label>
              <input
                type="tel"
                className="form-input"
                placeholder="VD: 0912345678"
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Ngày sinh</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1.35fr', gap: '8px' }}>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={2}
                  className="form-input"
                  placeholder="Ngày"
                  title="Ngày sinh (01 - 31)"
                  value={dobDay}
                  onChange={(e) => setDobDay(e.target.value.replace(/\D/g, '').slice(0, 2))}
                  style={{ textAlign: 'center', padding: '10px 4px' }}
                />
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={2}
                  className="form-input"
                  placeholder="Tháng"
                  title="Tháng sinh (01 - 12)"
                  value={dobMonth}
                  onChange={(e) => setDobMonth(e.target.value.replace(/\D/g, '').slice(0, 2))}
                  style={{ textAlign: 'center', padding: '10px 4px' }}
                />
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={4}
                  className="form-input"
                  placeholder="Năm"
                  title="Năm sinh (4 chữ số)"
                  value={dobYear}
                  onChange={(e) => setDobYear(e.target.value.replace(/\D/g, '').slice(0, 4))}
                  style={{ textAlign: 'center', padding: '10px 4px' }}
                />
              </div>
            </div>


            <div className="form-group">
              <label className="form-label">Giới tính</label>
              <div style={{ display: 'flex', gap: '20px', marginTop: '6px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', fontWeight: 600 }}>
                  <input
                    type="radio"
                    name="gender"
                    checked={gender === true}
                    onChange={() => setGender(true)}
                  />
                  Nam
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', fontWeight: 600 }}>
                  <input
                    type="radio"
                    name="gender"
                    checked={gender === false}
                    onChange={() => setGender(false)}
                  />
                  Nữ
                </label>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Tên đăng nhập (Hệ thống)</label>
              <input
                type="text"
                className="form-input"
                value={profile?.username || user?.username || ''}
                disabled
                style={{ background: 'var(--color-surface-muted)', cursor: 'not-allowed' }}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Email tài khoản</label>
              <input
                type="email"
                className="form-input"
                value={profile?.email || user?.email || ''}
                disabled
                style={{ background: 'var(--color-surface-muted)', cursor: 'not-allowed' }}
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '12px' }}
              disabled={savingProfile}
            >
              {savingProfile ? 'Đang lưu...' : 'Lưu thông tin'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
