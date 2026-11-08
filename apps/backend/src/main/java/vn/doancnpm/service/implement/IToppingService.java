package vn.doancnpm.service.implement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.domain.InventoryTopping;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.dto.Request.ToppingRequest;
import vn.doancnpm.dto.Response.ToppingResponse;
import vn.doancnpm.repository.InventoryToppingRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.service.ToppingService;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IToppingService implements ToppingService {

    private final ToppingRepository toppingRepository;
    private final InventoryToppingRepository inventoryToppingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ToppingResponse> getAllToppings(Boolean status) {
        List<Topping> toppings;
        // Nếu truyền status (true/false) thì lọc theo status, nếu null thì lấy tất cả
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Topping với ID: " + id));
        return mapToResponse(topping);
    }

    @Override
    @Transactional
    public ToppingResponse createTopping(ToppingRequest request) {
        // 1. Chuyển từ Request DTO sang Entity Topping
        Topping topping = Topping.builder()
                .name(request.getName())
                .unitPrice(request.getUnitPrice())
                .status(true)
                .build();
        Topping savedTopping = toppingRepository.save(topping);

        // 2. Khởi tạo tồn kho ban đầu (stock = 0)
        InventoryTopping initialStock = InventoryTopping.builder()
                .topping(savedTopping)
                .stock(0)
                .minThreshold(10)
                .build();
        inventoryToppingRepository.save(initialStock);

        // 3. Trả về Response DTO
        return mapToResponse(savedTopping, 0);
    }

    @Override
    @Transactional
    public ToppingResponse updateTopping(Long id, ToppingRequest request) {
        Topping existingTopping = toppingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Topping với ID: " + id));

        existingTopping.setName(request.getName());
        existingTopping.setUnitPrice(request.getUnitPrice());

        Topping updated = toppingRepository.save(existingTopping);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ToppingResponse updateStatus(Long id, boolean status) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Topping với ID: " + id));
        topping.setStatus(status);
        Topping updated = toppingRepository.save(topping);
        return mapToResponse(updated);
    }
    // --- HÀM PHỤ TRỢ CHUYỂN ENTITY SANG RESPONSE DTO ---
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