describe('Luồng E2E Production: Đăng nhập, Chọn món và Xác nhận thanh toán', () => {
  const BASE_URL = 'http://localhost:3000';

  beforeEach(() => {
    cy.visit(BASE_URL);
  });

  it('Khách hàng hoàn tất toàn bộ quy trình mua hàng trên hệ thống thật', () => {

    // ===============================================
    // KHAI BÁO LẮNG NGHE (SPY) CÁC API THẬT TỪ BACKEND
    // ===============================================
    cy.intercept('POST', '**/api/v1/auth/login*').as('loginApi');
    cy.intercept('GET', '**/api/v1/categories*').as('getCategories');
    cy.intercept('GET', '**/api/v1/products*').as('getProducts');
    cy.intercept('POST', '**/api/v1/orders/checkout*').as('checkoutReq');
    cy.intercept('POST', '**/api/v1/payments/request*').as('paymentRequestReq');
    cy.intercept('GET', '**/api/v1/orders*').as('getOrdersList');

    // Giữ mock cổng thanh toán nếu chưa nối cổng ngân hàng thật
    cy.intercept('GET', '**/mock-gateway/pay*', {
      statusCode: 200,
      body: { status: 'SUCCESS', message: 'Thanh toán thành công' }
    }).as('mockGateway');

    // ===============================================
    // BƯỚC 0: ĐĂNG NHẬP VỚI TÀI KHOẢN CUSTOMER THẬT (TỪ DATASEEDER)
    // ===============================================
    cy.contains('Đăng nhập').click({ force: true });
    cy.wait(2000);

    // Dùng tài khoản customer01 / Customer@123 khớp với DataSeeder.java
    cy.get('input[type="text"]').should('be.visible').clear().type('customer01');
    cy.get('input[type="password"]').should('be.visible').clear().type('Customer@123');
    cy.get('button[type="submit"]').contains('Đăng nhập').click({ force: true });

    cy.wait('@loginApi').its('response.statusCode').should('be.oneOf', [200, 201]);
    cy.wait(2000);

    // ===============================================
    // BƯỚC 1: TẠI TRANG MENU - TÌM VÀ CHỌN MÓN THẬT TRONG DB
    // ===============================================
    // Bấm nút "Khám phá thực đơn" lớn ở trang chủ (tránh bấm nhầm nút trong khung chat Mascot)
    cy.contains('a, button', 'Khám phá thực đơn').first().click({ force: true });
    cy.wait(2000);

    // Nếu vẫn chưa thấy ô tìm kiếm, bấm tiếp vào icon Thực đơn (nút thứ 2) trên thanh điều hướng dưới cùng
    cy.get('body').then(($body) => {
      if ($body.find('input[placeholder="Tìm món (VD: cà phê, trà...)"]').length === 0) {
        cy.get('nav a, nav button, .bottom-nav a, .bottom-nav button').eq(1).click({ force: true });
        cy.wait(2000);
      }
    });

    // Tìm "Ca Phe" (không dấu) khớp với "Ca Phe Sua Da Sai Gon" trong DataSeeder.java
    cy.get('input[placeholder="Tìm món (VD: cà phê, trà...)"]')
      .should('be.visible')
      .clear()
      .type('Ca Phe');
    cy.get('button[type="submit"]').contains('Tìm kiếm').click({ force: true });

    cy.wait('@getProducts').its('response.statusCode').should('eq', 200);
    cy.wait(2000);

    cy.get('.product-grid').should('be.visible');
    cy.get('.product-grid button').first().click({ force: true });
    cy.wait(2000);

    cy.get('button').contains('Thêm vào giỏ hàng').click({ force: true });
    cy.wait(2000);

    // ===============================================
    // BƯỚC 2: TẠI TRANG GIỎ HÀNG - XÁC NHẬN MÓN
    // ===============================================
    cy.get('button.cart-btn').click({ force: true });
    cy.wait(2000);

    cy.get('button').contains('Tiến hành thanh toán').click({ force: true });

    // ===============================================
    // BƯỚC 3: TẠI TRANG CHECKOUT - XÁC NHẬN ĐẶT HÀNG
    // ===============================================
    cy.get('input[placeholder="VD: Bàn 04"]').clear().type('Ban 09');
    cy.get('textarea[placeholder="VD: Ít ngọt, nhiều đá, để riêng đá..."]').clear().type('Cypress auto test');

    cy.scrollTo('top');
    cy.wait(2000);
    cy.get('button').contains('Xác nhận đặt hàng').click({ force: true });

    cy.wait('@checkoutReq').its('response.statusCode').should('be.oneOf', [200, 201]);
    cy.wait(3000);

    // ===============================================
    // BƯỚC 4: HOÀN TẤT THANH TOÁN VÀ XEM ĐƠN HÀNG ĐÃ THANH TOÁN
    // ===============================================
    cy.scrollTo('bottom');
    cy.wait(2000);

    // 1. Nhấn nút "Tôi đã hoàn tất thanh toán"
    cy.get('button').contains('Tôi đã hoàn tất thanh toán').scrollIntoView().should('be.visible').click({ force: true });
    cy.wait(3000);

    // 2. Nhấn nút "Theo dõi đơn hàng của tôi"
    cy.get('button, a').contains('Theo dõi đơn hàng của tôi').scrollIntoView().should('be.visible').click({ force: true });

    cy.wait('@getOrdersList').its('response.statusCode').should('eq', 200);
    cy.wait(3000);
  });
});