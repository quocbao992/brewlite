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
@Table(name = "topping_log")
@PrimaryKeyJoinColumn(name = "id_m")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ToppingLog extends InventoryLog {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "Id_T", nullable = false)
  private Topping topping;
}
