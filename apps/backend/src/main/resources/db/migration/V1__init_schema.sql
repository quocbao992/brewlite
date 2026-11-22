-- Clean Flyway Migration V1 (Compatible with both MySQL 8 and H2)

-- 1. Independent Tables (No Foreign Keys)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(30) NOT NULL,
  `status` bit(1) NOT NULL,
  `username` varchar(100) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
);

INSERT INTO `users` VALUES 
(26,'2026-11-05 10:15:03.445386','admin@brewlite.vn','$2a$10$jdziXVfpCnYf75sipSRELedTJk6OP2wfGnCkxgUz2JS/2ZdpdjFk.','ADMIN',1,'admin','Admin BrewLite'),
(27,'2026-11-05 10:15:03.567878','barista01@brewlite.vn','$2a$10$1isAJ6fv4rbz7oU5VXVcGukWMbRdY1N1/rGmqBpsSp4BRIWAzZz4q','BARISTA',1,'barista01','Nguyen Van An'),
(28,'2026-11-05 10:15:03.648374','cashier01@brewlite.vn','$2a$10$FH6jTJQJNUTcGFzODo04aeJOBKwQZdnfpzLNKf6rH2DaTaDxsjnTy','CASHIER',1,'cashier01','Tran Thi Binh'),
(29,'2026-11-05 10:15:03.732193','customer01@brewlite.vn','$2a$10$Qq3jMAVnLCKsArZdy5lPue/BXmPt8pyGsEjf.NKaC8FIT0MOoVxda','CUSTOMER',1,'customer01','Pham Quoc Bao'),
(30,'2026-11-05 10:15:03.815489','customer02@brewlite.vn','$2a$10$B1JQSjAPxWAtX1KHDkZ.O.KpDDu9vvo/8ni3PxJ/R1CMcZNq4FYL2','CUSTOMER',1,'customer02','Le Thi Mai'),
(31,'2026-11-05 10:15:03.918103','customer03@brewlite.vn','$2a$10$xdkg5keC4FwlkI5A9rqRmuCiN75aZkbj6sGLbINJ2BLOLfkuMioA6','CUSTOMER',1,'customer03','Nguyen Hoang Nam'),
(32,'2026-11-05 10:35:46.051025','ti@gmail.com','$2a$10$0PzQ39iqOCwGSpGiS7O0QOnjtjKayLpJxBaWQselmv33xT/IkrKA6','CUSTOMER',1,'Tony','Đạt');

DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `display_order` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  `status` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

INSERT INTO `category` VALUES 
(22,1,'Ca Phe Truyen Thong',1),
(23,2,'Espresso & Ca Phe Y',1),
(24,3,'Cold Brew Dac Biet',1),
(25,4,'Tra Trai Cay',1),
(26,5,'Banh Ngot An Kem',1);

DROP TABLE IF EXISTS `topping`;
CREATE TABLE `topping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `status` bit(1) NOT NULL,
  `unit_price` decimal(38,2) NOT NULL,
  PRIMARY KEY (`id`)
);

INSERT INTO `topping` VALUES 
(19,'Tran Chau Trang',1,10000.00),
(20,'Thach Ca Phe',1,10000.00),
(21,'Kem Cheese Beo',1,15000.00),
(22,'Sot Caramel',1,8000.00);

DROP TABLE IF EXISTS `promotion`;
CREATE TABLE `promotion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `discount_type` varchar(255) NOT NULL,
  `discount_value` decimal(38,2) NOT NULL,
  `end_date` datetime(6) NOT NULL,
  `max_discount` decimal(38,2) DEFAULT NULL,
  `min_order_amount` decimal(38,2) DEFAULT NULL,
  `start_date` datetime(6) NOT NULL,
  `usage_limit` int DEFAULT NULL,
  `used_count` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlomfalb6gsh66ox4gy0t2g7qw` (`code`)
);

-- 2. Level 1 Dependent Tables
DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_of_birth` date NOT NULL,
  `gender` bit(1) NOT NULL,
  `loyalty_point` float DEFAULT NULL,
  `phone_number` varchar(10) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6v6x92wb400iwh6unf5rwiim4` (`phone_number`),
  UNIQUE KEY `UKeuat1oase6eqv195jvb71a93s` (`user_id`),
  CONSTRAINT `FKrh1g1a20omjmn6kurd35o3eit` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

INSERT INTO `customers` VALUES 
(14,'2000-01-01',1,120,'0901111003',29),
(15,'2000-01-01',1,120,'0901111004',30),
(16,'2000-01-01',1,120,'0901111005',31),
(17,'2005-06-27',1,0,'0900904567',32);

DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `img_url` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `status` bit(1) NOT NULL,
  `unit_price` decimal(38,2) NOT NULL,
  `category_id` bigint NOT NULL,
  `version` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKopy87tn4r9vtox206gc4jtaqj` (`img_url`),
  KEY `FK1mtsbur82frn64de7balymq9s` (`category_id`),
  CONSTRAINT `FK1mtsbur82frn64de7balymq9s` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
);

INSERT INTO `product` VALUES 
(2,'cf-sua-da.jpg','Ca Phe Sua Da Sai Gon',1,29000.00,22,0),
(3,'cf-den-da.jpg','Ca Phe Den Da Dam Vi',1,25000.00,22,0),
(4,'bac-xiu.jpg','Bac Xiu Kem Sua 3 Tang',1,35000.00,22,0),
(5,'caffe-latte.jpg','Caffe Latte Thom Beo',1,45000.00,23,0),
(6,'caramel-macchiato.jpg','Caramel Macchiato',1,49000.00,23,0),
(7,'cold-brew.jpg','Cold Brew Cam Sa Mat Lanh',1,48000.00,24,0),
(8,'tra-dao.jpg','Tra Dao Cam Sa Tuoi Mat',1,39000.00,25,0),
(9,'croissant.jpg','Banh Sung Bo Croissant Bo Phap',1,32000.00,26,0);

DROP TABLE IF EXISTS `inventory_topping`;
CREATE TABLE `inventory_topping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `min_threshold` int DEFAULT NULL,
  `stock` int NOT NULL,
  `warehouse_stock` int NOT NULL,
  `topping_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKh2rjbcu1jdv2gi2keaagx74y1` (`topping_id`),
  CONSTRAINT `FK1fwqkv99mh13w0j5w0j6rm2vs` FOREIGN KEY (`topping_id`) REFERENCES `topping` (`id`)
);

INSERT INTO `inventory_topping` VALUES 
(17,10,200,200,19),
(18,10,200,200,20),
(19,10,200,200,21),
(20,10,200,200,22);

-- 3. Level 2 Dependent Tables
DROP TABLE IF EXISTS `carts`;
CREATE TABLE `carts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `status` bit(1) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8ba3sryid5k8a9kidpkvqipyt` (`customer_id`),
  CONSTRAINT `FK8ba3sryid5k8a9kidpkvqipyt` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
);

DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `discount_amount` decimal(38,2) NOT NULL,
  `loyalty_points_used` int NOT NULL,
  `status` enum('CANCELLED','COMPLETED','PAID','PAYMENT_FAILED','PENDING','PREPARING','READY') NOT NULL,
  `subtotal` decimal(38,2) NOT NULL,
  `total_amount` decimal(38,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `cancel_reason` varchar(255) DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `order_code` varchar(30) DEFAULT NULL,
  `order_type` enum('DINE_IN','TAKE_AWAY') DEFAULT NULL,
  `payment_method` enum('CASH','MOMO','VNPAY') DEFAULT NULL,
  `table_number` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdhk2umg8ijjkg4njg6891trit` (`order_code`),
  KEY `FKpxtb8awmi0dk6smoh2vp1litg` (`customer_id`),
  CONSTRAINT `FKpxtb8awmi0dk6smoh2vp1litg` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
);

INSERT INTO `orders` VALUES 
(1,'2026-11-05 10:15:04.112437',0.00,0,'PAID',93000.00,93000.00,'2026-11-05 10:15:04.112437',14,NULL,NULL,'ORD-20261028-001','DINE_IN',NULL,'Ban 02'),
(2,'2026-11-05 10:15:04.125109',0.00,0,'PREPARING',90000.00,90000.00,'2026-11-05 10:15:04.125109',15,NULL,NULL,'ORD-20261028-002','TAKE_AWAY',NULL,NULL),
(3,'2026-11-05 10:15:04.130106',0.00,0,'READY',54000.00,54000.00,'2026-11-05 10:15:04.130106',16,NULL,NULL,'ORD-20261028-003','DINE_IN',NULL,'Ban 05'),
(4,'2026-11-05 10:15:04.140205',0.00,0,'COMPLETED',35000.00,35000.00,'2026-11-05 10:15:04.140205',14,NULL,NULL,'ORD-20261028-004','TAKE_AWAY',NULL,NULL),
(5,'2026-11-05 10:15:04.145719',0.00,0,'CANCELLED',45000.00,45000.00,'2026-11-05 10:15:04.145719',15,NULL,NULL,'ORD-20261028-005','DINE_IN',NULL,'Ban 01');

DROP TABLE IF EXISTS `inventory_product`;
CREATE TABLE `inventory_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `min_threshold` int DEFAULT NULL,
  `stock` int NOT NULL,
  `warehouse_stock` int NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgcwdcrd9ro80es6macuuduva2` (`product_id`),
  CONSTRAINT `FK47vtq82lhhheejnn75sundyo6` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
);

INSERT INTO `inventory_product` VALUES 
(1,5,100,100,2),
(2,5,100,100,3),
(3,5,100,100,4),
(4,5,80,100,5),
(5,5,80,100,6),
(6,5,60,100,7),
(7,5,90,100,8),
(8,5,50,100,9);

DROP TABLE IF EXISTS `product_available_topping`;
CREATE TABLE `product_available_topping` (
  `product_id` bigint NOT NULL,
  `available_topping_id` bigint NOT NULL,
  KEY `FKeajf6jd5eycl614r9k003lbw9` (`available_topping_id`),
  KEY `FKtgi924u3yygsunr42wxwxktt5` (`product_id`),
  CONSTRAINT `FKeajf6jd5eycl614r9k003lbw9` FOREIGN KEY (`available_topping_id`) REFERENCES `topping` (`id`),
  CONSTRAINT `FKtgi924u3yygsunr42wxwxktt5` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
);

DROP TABLE IF EXISTS `producttopping`;
CREATE TABLE `producttopping` (
  `productid` bigint NOT NULL,
  `toppingid` bigint NOT NULL,
  KEY `FK42j5ut4l699g361vx2rlciivd` (`toppingid`),
  KEY `FK7p9l7icof333fccc0xcopkchw` (`productid`),
  CONSTRAINT `FK42j5ut4l699g361vx2rlciivd` FOREIGN KEY (`toppingid`) REFERENCES `topping` (`id`),
  CONSTRAINT `FK7p9l7icof333fccc0xcopkchw` FOREIGN KEY (`productid`) REFERENCES `product` (`id`)
);

-- 4. Level 3 Dependent Tables
DROP TABLE IF EXISTS `cart_items`;
CREATE TABLE `cart_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `quantity` int NOT NULL,
  `size` enum('L','M','S') DEFAULT NULL,
  `unit_price` decimal(38,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `cart_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `note` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpcttvuq4mxppo8sxggjtn5i2c` (`cart_id`),
  KEY `FKl7je3auqyq1raj52qmwrgih8x` (`product_id`),
  CONSTRAINT `FKl7je3auqyq1raj52qmwrgih8x` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`)
);

DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `line_total` decimal(38,2) NOT NULL,
  `quantity` int NOT NULL,
  `size` varchar(255) NOT NULL,
  `unit_price` decimal(38,2) NOT NULL,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(255) NOT NULL,
  `product_image` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  KEY `FKlf6f9q956mt144wiv6p1yko16` (`product_id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKlf6f9q956mt144wiv6p1yko16` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
);

INSERT INTO `order_items` VALUES 
(1,58000.00,2,'M',29000.00,1,2,'Ca Phe Sua Da Sai Gon','cf-sua-da.jpg'),
(2,35000.00,1,'L',35000.00,1,4,'Bac Xiu Kem Sua 3 Tang','bac-xiu.jpg'),
(3,90000.00,2,'L',45000.00,2,5,'Caffe Latte Thom Beo','caffe-latte.jpg'),
(4,25000.00,1,'M',25000.00,3,3,'Ca Phe Den Da Dam Vi','cf-den-da.jpg'),
(5,29000.00,1,'L',29000.00,3,2,'Ca Phe Sua Da Sai Gon','cf-sua-da.jpg'),
(6,35000.00,1,'M',35000.00,4,4,'Bac Xiu Kem Sua 3 Tang','bac-xiu.jpg'),
(7,45000.00,1,'M',45000.00,5,5,'Caffe Latte Thom Beo','caffe-latte.jpg');

DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(38,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `idempotency_key` varchar(255) NOT NULL,
  `method` enum('Momo','Stripe','VNPay') NOT NULL,
  `paid_at` datetime(6) NOT NULL,
  `transaction_reference` varchar(255) NOT NULL,
  `order_id` bigint NOT NULL,
  `status` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3htyuipi3b6is4aors6n0ah9i` (`idempotency_key`),
  KEY `FKlouu98csyullos9k25tbpk4va` (`order_id`),
  CONSTRAINT `FKlouu98csyullos9k25tbpk4va` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
);

DROP TABLE IF EXISTS `loyalty_transaction`;
CREATE TABLE `loyalty_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `points` int NOT NULL,
  `type` varchar(255) NOT NULL,
  `customer_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpnf15ewifk5tcvx9c5pl13554` (`order_id`,`type`),
  KEY `FKfie6tevy25t8goq97surovf2x` (`customer_id`),
  CONSTRAINT `FKfie6tevy25t8goq97surovf2x` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKiqdhda3dwee698bwxfbjgrswg` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
);

DROP TABLE IF EXISTS `inventory_log`;
CREATE TABLE `inventory_log` (
  `id_m` bigint NOT NULL AUTO_INCREMENT,
  `change_type` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `quantity_change` int NOT NULL,
  `reason` varchar(255) NULL,
  `id_o` bigint DEFAULT NULL,
  `id_p` bigint DEFAULT NULL,
  PRIMARY KEY (`id_m`),
  KEY `FKfkooumspyh2wgbvygp9ycexof` (`id_o`),
  KEY `FKt75ysnfw52ora6eks6bfv37g9` (`id_p`),
  CONSTRAINT `FKfkooumspyh2wgbvygp9ycexof` FOREIGN KEY (`id_o`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKt75ysnfw52ora6eks6bfv37g9` FOREIGN KEY (`id_p`) REFERENCES `product` (`id`)
);

-- 5. Level 4 Dependent Tables
DROP TABLE IF EXISTS `cart_item_toppings`;
CREATE TABLE `cart_item_toppings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price` decimal(38,2) NOT NULL,
  `quantity` int NOT NULL,
  `cart_item_id` bigint NOT NULL,
  `topping_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7qpc6ofhbvkroff8h1aexla8p` (`cart_item_id`),
  KEY `FKputmecf9q7gr1rt0bp8jqyfw3` (`topping_id`),
  CONSTRAINT `FK7qpc6ofhbvkroff8h1aexla8p` FOREIGN KEY (`cart_item_id`) REFERENCES `cart_items` (`id`),
  CONSTRAINT `FKputmecf9q7gr1rt0bp8jqyfw3` FOREIGN KEY (`topping_id`) REFERENCES `topping` (`id`)
);

DROP TABLE IF EXISTS `order_item_toppings`;
CREATE TABLE `order_item_toppings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `unit_price` decimal(38,2) NOT NULL,
  `order_item_id` bigint NOT NULL,
  `topping_id` bigint NOT NULL,
  `topping_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqonqgw2q2blcduhtbkekq27wr` (`order_item_id`),
  KEY `FKotphm1tya2bk19neleobwexaa` (`topping_id`),
  CONSTRAINT `FKotphm1tya2bk19neleobwexaa` FOREIGN KEY (`topping_id`) REFERENCES `topping` (`id`),
  CONSTRAINT `FKqonqgw2q2blcduhtbkekq27wr` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`)
);

DROP TABLE IF EXISTS `product_log`;
CREATE TABLE `product_log` (
  `id_m` bigint NOT NULL,
  `id_p` bigint NOT NULL,
  PRIMARY KEY (`id_m`),
  KEY `FKq8xfh75o6c3iidqd26mg5oc0p` (`id_p`),
  CONSTRAINT `FKpmyskkyofphhw0jx2s6qqpxhd` FOREIGN KEY (`id_m`) REFERENCES `inventory_log` (`id_m`),
  CONSTRAINT `FKq8xfh75o6c3iidqd26mg5oc0p` FOREIGN KEY (`id_p`) REFERENCES `product` (`id`)
);

DROP TABLE IF EXISTS `topping_log`;
CREATE TABLE `topping_log` (
  `id_m` bigint NOT NULL,
  `id_t` bigint NOT NULL,
  PRIMARY KEY (`id_m`),
  KEY `FK1x89sm6qcpawv3x087f714v17` (`id_t`),
  CONSTRAINT `FK1x89sm6qcpawv3x087f714v17` FOREIGN KEY (`id_t`) REFERENCES `topping` (`id`),
  CONSTRAINT `FKf5fcrboxebfv1enyo0h4vsqhm` FOREIGN KEY (`id_m`) REFERENCES `inventory_log` (`id_m`)
);