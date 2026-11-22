import React from 'react';
import { formatService } from '../../services/formatService';

export const ToppingCheckboxList = ({
  toppings = [],
  selectedToppings = [],
  onToggle,
  allowUncheckDisabled = false,
  maxHeight = '140px',
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '6px',
        maxHeight,
        overflowY: 'auto',
        background: '#f8fafc',
        padding: '10px',
        borderRadius: '8px',
        border: '1px solid var(--color-border, #e2e8f0)',
      }}
    >
      {toppings.map((tp) => {
        const isChecked = selectedToppings.some((t) => (t.id || t.toppingId) === tp.id);
        const { isOutOfStock, isDisabled, label } = formatService.getToppingAvailability(tp);
        const checkboxDisabled = isDisabled && !(allowUncheckDisabled && isChecked);
        const price = Number(tp.price ?? tp.unitPrice) || 0;

        return (
          <label
            key={tp.id}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '6px 8px',
              borderRadius: '6px',
              background: isChecked ? '#f1f5f9' : 'transparent',
              cursor: checkboxDisabled ? 'not-allowed' : 'pointer',
              opacity: isDisabled ? 0.65 : 1,
              fontSize: '0.85rem',
            }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <input
                type="checkbox"
                checked={isChecked}
                disabled={checkboxDisabled}
                onChange={() => {
                  if (!checkboxDisabled) {
                    onToggle(tp);
                  }
                }}
                style={{
                  cursor: checkboxDisabled ? 'not-allowed' : 'pointer',
                  accentColor: 'var(--color-accent, #c17838)',
                }}
              />
              <span
                style={{
                  textDecoration: isDisabled ? 'line-through' : 'none',
                  color: isDisabled ? '#64748b' : 'inherit',
                  fontWeight: isChecked ? 600 : 400,
                }}
              >
                {tp.name}
              </span>
              {label && (
                <span
                  style={{
                    fontSize: '0.72rem',
                    fontWeight: 600,
                    padding: '1px 6px',
                    borderRadius: '999px',
                    background: isOutOfStock ? '#fef2f2' : '#e2e8f0',
                    color: isOutOfStock ? '#ef4444' : '#475569',
                    border: isOutOfStock ? '1px solid #fecaca' : '1px solid #cbd5e1',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {label}
                </span>
              )}
            </span>
            <span
              style={{
                color: isDisabled ? '#94a3b8' : 'var(--color-accent, #c17838)',
                fontWeight: 600,
                whiteSpace: 'nowrap',
              }}
            >
              +{formatService.formatMoney(price)}
            </span>
          </label>
        );
      })}
    </div>
  );
};

export default ToppingCheckboxList;
