package vn.doancnpm.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DynamicInsert
@Table(name = "product")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "img_URL", nullable = false, unique = true)
    private String imgURL;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private boolean status = true;

    @ManyToMany // liên kết phần topping với MP
    @JoinTable(name = "producttopping", joinColumns = @JoinColumn(name = "productid"), inverseJoinColumns = @JoinColumn(name = "toppingid"))
    @Builder.Default
    private List<Topping> availableTopping = new ArrayList<>();

    @Version
    private Long version;
}
