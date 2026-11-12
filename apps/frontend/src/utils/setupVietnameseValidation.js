export function setupVietnameseValidation() {
  if (typeof window === 'undefined' || typeof document === 'undefined') return;

  const handleInvalid = (e) => {
    const target = e.target;
    if (!target || typeof target.setCustomValidity !== 'function') return;

    const validity = target.validity;
    if (!validity) return;

    if (validity.valueMissing) {
      if (target.dataset?.errorRequired) {
        target.setCustomValidity(target.dataset.errorRequired);
      } else if (target.tagName === 'SELECT') {
        target.setCustomValidity('Vui lòng chọn một mục trong danh sách.');
      } else if (target.type === 'checkbox') {
        target.setCustomValidity('Vui lòng đánh dấu vào ô này nếu bạn muốn tiếp tục.');
      } else if (target.type === 'radio') {
        target.setCustomValidity('Vui lòng chọn một tùy chọn.');
      } else {
        target.setCustomValidity('Vui lòng điền vào trường này.');
      }
    } else if (validity.rangeUnderflow) {
      if (target.dataset?.errorMin) {
        target.setCustomValidity(target.dataset.errorMin);
      } else {
        const minVal = target.min !== '' ? target.min : '1';
        target.setCustomValidity(`Giá trị phải lớn hơn hoặc bằng ${minVal}.`);
      }
    } else if (validity.rangeOverflow) {
      if (target.dataset?.errorMax) {
        target.setCustomValidity(target.dataset.errorMax);
      } else {
        target.setCustomValidity(`Giá trị phải nhỏ hơn hoặc bằng ${target.max}.`);
      }
    } else if (validity.typeMismatch) {
      if (target.type === 'email') {
        target.setCustomValidity('Vui lòng nhập địa chỉ email hợp lệ.');
      } else if (target.type === 'url') {
        target.setCustomValidity('Vui lòng nhập đường dẫn URL hợp lệ.');
      } else {
        target.setCustomValidity('Vui lòng nhập đúng định dạng yêu cầu.');
      }
    } else if (validity.tooShort) {
      const curLen = target.value ? target.value.length : 0;
      target.setCustomValidity(`Vui lòng nhập tối thiểu ${target.minLength} ký tự (hiện tại có ${curLen} ký tự).`);
    } else if (validity.tooLong) {
      target.setCustomValidity(`Vui lòng nhập không vượt quá ${target.maxLength} ký tự.`);
    } else if (validity.stepMismatch) {
      target.setCustomValidity('Vui lòng nhập giá trị hợp lệ theo bước nhảy.');
    } else if (validity.patternMismatch) {
      target.setCustomValidity('Dữ liệu không khớp với định dạng được yêu cầu.');
    }
  };

  const handleClear = (e) => {
    const target = e.target;
    if (target && typeof target.setCustomValidity === 'function') {
      target.setCustomValidity('');
    }
  };

  document.addEventListener('invalid', handleInvalid, true);
  document.addEventListener('input', handleClear, true);
  document.addEventListener('change', handleClear, true);
}

export default setupVietnameseValidation;
