# 🌸 Chiikawa Trio Pixel Art Asset Package

Bộ asset Pixel Art chuẩn Anime của bộ ba **Chiikawa**, **Usagi**, và **Hachiware** được thiết kế tỉ mỉ, độ nét pixel nguyên bản (crisp edges, không làm mờ), dáng đứng chuẩn 0.0°, mắt long lanh 2 đốm sáng trắng và hoạt ảnh 4 frame đi bộ luân phiên (Right forward -> Passing -> Left forward -> Passing).

---

## 📁 Cấu Trúc Thư Mục Asset

```text
Web/image/chiikawa/
├── README.md                     # Hướng dẫn chi tiết sử dụng
├── spritesheet.png               # Master Spritesheet (675x438, 3 hàng x 5 cột)
├── spritesheet.json              # Tọa độ bounding box từng frame (JSON Atlas)
├── chiikawa_sprites.js           # Module ES6 / Window chứa toàn bộ Base64 PNG
├── chiikawa_sprites.json         # File JSON từ điển Base64 của tất cả 15 sprite
│
├── trio_standing.png             # Cả bộ ba đứng thẳng cùng nhau (Transparent PNG)
├── trio_walk.gif                 # Hoạt ảnh cả bộ ba đi bộ lặp lại mượt mà (GIF)
│
├── chiikawa_walk.gif             # Hoạt ảnh đi bộ bé Chiikawa (GIF)
├── chiikawa_walk_strip.png       # Dải 4 frame bước chân Chiikawa
├── chiikawa/
│   ├── idle.png                  # Đứng thở chuẩn (125x109)
│   ├── walk_rfwd.png             # Chân phải bước ra trước, chân trái ở sau
│   ├── walk_lfwd.png             # Chân trái bước ra trước, chân phải ở sau
│   ├── walk_lfwd_left.png        # Bước sang trái (chân trái trước)
│   └── walk_rfwd_left.png        # Bước sang trái (chân phải trước)
│
├── usagi_walk.gif                # Hoạt ảnh đi bộ thỏ Usagi (GIF)
├── usagi_walk_strip.png          # Dải 4 frame bước chân Usagi
├── usagi/
│   ├── idle.png                  # Đứng thở chuẩn (100x142)
│   ├── walk_rfwd.png             # Chân phải bước ra trước, chân trái ở sau
│   ├── walk_lfwd.png             # Chân trái bước ra trước, chân phải ở sau
│   ├── walk_lfwd_left.png        # Bước sang trái (chân trái trước)
│   └── walk_rfwd_left.png        # Bước sang trái (chân phải trước)
│
├── hachiware_walk.gif            # Hoạt ảnh đi bộ mèo Hachiware (GIF)
├── hachiware_walk_strip.png      # Dải 4 frame bước chân Hachiware
└── hachiware/
    ├── idle.png                  # Đứng thở chuẩn (128x120)
    ├── walk_rfwd.png             # Chân phải bước ra trước, chân trái ở sau
    ├── walk_lfwd.png             # Chân trái bước ra trước, chân phải ở sau
    ├── walk_lfwd_left.png        # Bước sang trái (chân trái trước)
    └── walk_rfwd_left.png        # Bước sang trái (chân phải trước)
```

---

## 🚀 Cách Sử Dụng Trong Dự Án Web

### 1. Dùng Trực Tiếp Ảnh Động (Dễ nhất cho Loading Screen / Modal)
```html
<!-- Dùng cả bộ ba đi bộ cho màn hình Loading / Sync DB -->
<div class="loading-overlay">
  <img src="/image/chiikawa/trio_walk.gif" alt="Chiikawa Trio Loading" style="image-rendering: pixelated; width: 320px;">
  <p>Đang đồng bộ cơ sở dữ liệu AOTR...</p>
</div>

<!-- Hoặc dùng từng bé riêng lẻ -->
<img src="/image/chiikawa/usagi_walk.gif" style="image-rendering: pixelated; width: 100px;">
```

### 2. Dùng CSS Spritesheet (`spritesheet.png`)
Mỗi ô sprite có kích thước chuẩn **135px x 146px**:
* **Hàng 0 (Y: 0px)**: Chiikawa
* **Hàng 1 (Y: 146px)**: Usagi
* **Hàng 2 (Y: 292px)**: Hachiware
* **Cột 0**: Idle | **Cột 1**: Walk R1 | **Cột 2**: Walk L1 | **Cột 3**: Walk Left 1 | **Cột 4**: Walk Left 2

```css
.chiikawa-sprite {
  width: 135px;
  height: 146px;
  background-image: url('/image/chiikawa/spritesheet.png');
  background-repeat: no-repeat;
  image-rendering: pixelated;
}

/* Bé Chiikawa đứng thở */
.chiikawa-idle { background-position: 0px 0px; }

/* Thỏ Usagi đứng thở */
.usagi-idle    { background-position: 0px -146px; }

/* Mèo Hachiware đứng thở */
.hachiware-idle{ background-position: 0px -292px; }
```

### 3. Dùng Module JavaScript Base64 (Nhúng tức thì không lo lỗi đường dẫn)
```html
<script src="/image/chiikawa/chiikawa_sprites.js"></script>
<script>
  // Truy cập trực tiếp ảnh Base64
  const usagiIdleSrc = window.CHIIKAWA_SPRITES.usagi_idle;
  const chiiWalkSrc  = window.CHIIKAWA_SPRITES.chiikawa_walk_rfwd;
  
  const img = new Image();
  img.src = usagiIdleSrc;
  document.body.appendChild(img);
</script>
```

---

## 🎨 Thông Số Kỹ Thuật (Specs)
* **Phong cách**: Retro Anime Pixel Art (Chuẩn 100% tỷ lệ gốc).
* **Màu sắc**: Màu anime nguyên bản, mắt double-highlight lấp lánh, miệng chữ "w".
* **Độ trong suốt**: Nền Transparent (Alpha Channel 0) 100%.
* **Dáng đứng**: Thẳng tự nhiên **0.0°**.
