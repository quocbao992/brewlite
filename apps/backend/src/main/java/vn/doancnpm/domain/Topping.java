package vn.doancnpm.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "topping")
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter 
@Builder 
public class Topping {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "unit_price",nullable = false)
    private BigDecimal unitPrice;

    // BỔ SUNG: Số lượng tồn kho
    /*@Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0;
    */
    @Column(name = "status",nullable = false)
    @Builder.Default
    private Boolean status=true;
}
