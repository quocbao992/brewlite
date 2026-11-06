package vn.doancnpm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "size", nullable = false)
  private String size;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  @Column(name = "line_total", nullable = false)
  private BigDecimal lineTotal;

  @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItemTopping> toppings;

  public List<OrderItemTopping> getToppings() {
    if (toppings == null) {
      toppings = new ArrayList<>();
    }
    return toppings;
  }

  // them
  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "product_image")
  private String productImage;
}
