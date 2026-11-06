package vn.doancnpm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items;

  public List<OrderItem> getItems() {
    if (items == null) {
      items = new ArrayList<>();
    }
    return items;
  }

  @Column(name = "subtotal", nullable = false)
  private BigDecimal subtotal;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount;

  @Column(name = "loyalty_points_used", nullable = false)
  private int loyaltyPointsUsed;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public enum Status {
    PENDING,
    PAID,
    PREPARING,
    READY,
    COMPLETED,
    PAYMENT_FAILED,
    CANCELLED
  }

  public enum OrderType {
    DINE_IN,
    TAKE_AWAY
  }

  // public enum PaymentMethod { VNPAY, MOMO, CASH }

  @Column(unique = true, length = 30)
  private String orderCode;

  @Enumerated(EnumType.STRING)
  private OrderType orderType;

  private String tableNumber;

  // @Enumerated(EnumType.STRING)
  // private Payment paymentMethod;

  @Column(length = 500)
  private String note;

  private String cancelReason;
}
