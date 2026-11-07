import React, { useState } from 'react';

export const ChiikawaMascot = ({
  message = 'Bạn có thể chọn topping và size khi ấn đặt món',
  character = 'chiikawa',
  onActionClick,
  actionText,
  style = {},
}) => {
  const [currentChar, setCurrentChar] = useState(character);
  const [isWaving, setIsWaving] = useState(false);

  const mascotGifs = {
    chiikawa: '/chiikawa/chiikawa_walk.gif',
    hachiware: '/chiikawa/hachiware_walk.gif',
    usagi: '/chiikawa/usagi_walk.gif',
    trio: '/chiikawa/trio_walk.gif',
  };

  const mascotNames = {
    chiikawa: 'Bé Chiikawa',
    hachiware: 'Mèo Hachiware',
    usagi: 'Thỏ Usagi',
    trio: 'Bộ ba Chiikawa',
  };

  const handleMascotChange = () => {
    setIsWaving(true);
    setTimeout(() => setIsWaving(false), 600);

    const chars = ['chiikawa', 'hachiware', 'usagi', 'trio'];
    const nextIdx = (chars.indexOf(currentChar) + 1) % chars.length;
    setCurrentChar(chars[nextIdx]);
  };

  const imgSrc = mascotGifs[currentChar] || mascotGifs.chiikawa;

  return (
    <div
      className="chiikawa-mascot-floating"
      style={{
        position: 'fixed',
        bottom: '90px',
        right: '24px',
        zIndex: 1000,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-end',
        gap: '6px',
        pointerEvents: 'none',
        ...style,
      }}
    >
      <div style={{ pointerEvents: 'auto' }}>
        <button
          type="button"
          onClick={handleMascotChange}
          style={{
            background: '#ffffff',
            color: '#92400e',
            border: '1.5px solid #fcd34d',
            borderRadius: '16px',
            padding: '4px 10px',
            fontSize: '0.75rem',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
            transition: 'all 0.2s ease',
          }}
        >
          Đổi mascot
        </button>
      </div>

      {message && (
        <div
          style={{
            pointerEvents: 'auto',
            background: '#ffffff',
            padding: '10px 14px',
            borderRadius: '14px',
            border: '1.5px solid #fcd34d',
            boxShadow: '0 8px 24px rgba(0,0,0,0.14)',
            fontSize: '0.85rem',
            fontWeight: 600,
            color: '#92400e',
            maxWidth: '260px',
            position: 'relative',
            lineHeight: 1.45,
          }}
        >
          <div>{message}</div>

          {actionText && onActionClick && (
            <div style={{ marginTop: '8px', textAlign: 'right' }}>
              <button
                type="button"
                onClick={onActionClick}
                style={{
                  background: '#c17838',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '4px 10px',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                {actionText}
              </button>
            </div>
          )}

          <div
            style={{
              position: 'absolute',
              bottom: '-7px',
              right: '26px',
              width: 0,
              height: 0,
              borderLeft: '7px solid transparent',
              borderRight: '7px solid transparent',
              borderTop: '8px solid #fcd34d',
            }}
          />
          <div
            style={{
              position: 'absolute',
              bottom: '-5px',
              right: '27px',
              width: 0,
              height: 0,
              borderLeft: '6px solid transparent',
              borderRight: '6px solid transparent',
              borderTop: '7px solid #ffffff',
            }}
          />
        </div>
      )}

      <div
        onClick={handleMascotChange}
        style={{
          pointerEvents: 'auto',
          cursor: 'pointer',
          userSelect: 'none',
          transform: isWaving ? 'scale(1.15) rotate(6deg)' : 'scale(1)',
          transition: 'transform 0.2s ease',
        }}
        title={mascotNames[currentChar]}
      >
        <img
          src={imgSrc}
          alt={mascotNames[currentChar]}
          style={{
            height: currentChar === 'trio' ? '54px' : '64px',
            imageRendering: 'pixelated',
            objectFit: 'contain',
            filter: 'drop-shadow(0 4px 8px rgba(0,0,0,0.2))',
          }}
        />
      </div>
    </div>
  );
};

export default ChiikawaMascot;
