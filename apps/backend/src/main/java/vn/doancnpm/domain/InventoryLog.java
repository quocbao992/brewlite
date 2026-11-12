package vn.doancnpm.domain;

import java.time.LocalDateTime;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_m")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Id_O", nullable = true)
    private Order order;

    @Column(name = "QuantityChange", nullable = false)
    private int quantityChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "ChangeType", nullable = false)
    private ChangeType changeType;

    @Column(name = "reason")
    private String reason;
    @Column(name = "createdAt", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ChangeType {
    IMPORT,        // nhập hàng / cộng kho
    ORDER_DEDUCT,   // khách đặt hàng, trừ kho
    TRANSFER,       // chuyển kho -> quầy
    
}
}

