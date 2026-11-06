package vn.doancnpm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product_log")
@PrimaryKeyJoinColumn(name = "id_m")
@Getter 
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLog extends InventoryLog {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Id_P", nullable = false)
    private Product product;
}

