package vn.doancnpm.service.implement;
import java.math.BigDecimal;                              
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;       
import lombok.RequiredArgsConstructor;
import vn.doancnpm.domain.InventoryTopping;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.dto.Request.ToppingRequest;
import vn.doancnpm.dto.Response.ToppingResponse;
import vn.doancnpm.repository.InventoryToppingRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.service.ToppingService;

@Service
@RequiredArgsConstructor
public class IToppingService implements ToppingService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000000");

    private final ToppingRepository toppingRepository;
    private final InventoryToppingRepository inventoryToppingRepository;

    private String validateName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên topping không được để trống");
        }
        String name = rawName.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Tên topping không được dài quá " + MAX_NAME_LENGTH + " ký tự");
        }
        return name;
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá topping không được âm");
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Giá topping chỉ được có tối đa 2 chữ số thập phân");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Giá topping không được vượt quá 1.000.000.000");
        }
        return price;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ToppingResponse> getAllToppings(Boolean status) {
        List<Topping> toppings;
        if (status != null) {
            toppings = toppingRepository.findByStatus(status);
        } else {
            toppings = toppingRepository.findAll();
        }

        return toppings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ToppingResponse getToppingById(Long id) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Topping với ID: " + id)); // SỬA
        return mapToResponse(topping);
    }

    @Override
    @Transactional
    public ToppingResponse createTopping(ToppingRequest request) {
        // ===== SỬA: validate + check trùng tên trước khi tạo =====
        String name = validateName(request.getName());
        BigDecimal price = validatePrice(request.getUnitPrice());

        if (toppingRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên topping đã tồn tại");
        }

        Topping topping = Topping.builder()
                .name(name)          // SỬA: dùng name đã trim
                .unitPrice(price)    // SỬA: dùng price đã validate
                .status(true)
                .build();
        Topping savedTopping = toppingRepository.save(topping);

        InventoryTopping initialStock = InventoryTopping.builder()
                .topping(savedTopping)
                .stock(0)
                .minThreshold(10)
                .build();
        inventoryToppingRepository.save(initialStock);

        return mapToResponse(savedTopping, 0);
    }

    @Override
    @Transactional
    public ToppingResponse updateTopping(Long id, ToppingRequest request) {
        Topping existingTopping = toppingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Topping với ID: " + id)); // SỬA

        String name = validateName(request.getName());
        BigDecimal price = validatePrice(request.getUnitPrice());

        if (toppingRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Tên topping đã tồn tại");
        }

        existingTopping.setName(name);
        existingTopping.setUnitPrice(price);

        Topping updated = toppingRepository.save(existingTopping);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ToppingResponse updateStatus(Long id, boolean status) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Topping với ID: " + id)); // SỬA
        topping.setStatus(status);
        Topping updated = toppingRepository.save(topping);
        return mapToResponse(updated);
    }

    // --- HÀM PHỤ TRỢ CHUYỂN ENTITY SANG RESPONSE DTO (giữ nguyên) ---
    private ToppingResponse mapToResponse(Topping topping) {
        Integer currentStock = inventoryToppingRepository.findByToppingId(topping.getId())
                .map(InventoryTopping::getStock)
                .orElse(0);
        return mapToResponse(topping, currentStock);
    }

    private ToppingResponse mapToResponse(Topping topping, Integer stock) {
        return ToppingResponse.builder()
                .id(topping.getId())
                .name(topping.getName())
                .unitPrice(topping.getUnitPrice())
                .stock(stock)
                .status(topping.getStatus())
                .build();
    }
}