package vn.doancnpm.dto.Request;

import jakarta.persistence.Column;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartRequest {

  private Long customerId;

  @Builder.Default
  @Column(name = "status")
  private Boolean status = true;

  private CartItemRequest item;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
