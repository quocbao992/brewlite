package vn.doancnpm.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.dto.Request.CategoryRequest;
import vn.doancnpm.dto.Response.CategoryResponse;
import vn.doancnpm.service.CategoryService;

@RestController
@RequestMapping(value = "/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@RequestBody CategoryRequest request) {
        return new ResponseEntity<>(categoryService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @PatchMapping("/{id}/hide")
    public ResponseEntity<CategoryResponse> hide(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.hide(id));
    }

    @PatchMapping("/{id}/show")
    public ResponseEntity<CategoryResponse> show(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.show(id));
    }

    @PatchMapping("/{id}/display-order")
    public ResponseEntity<CategoryResponse> updateDisplayOrder(@PathVariable Long id,
    @RequestParam("value") Long value) {
        return ResponseEntity.ok(categoryService.updateDisplayOrder(id, value));
    }

    @GetMapping("/menu")
    public ResponseEntity<
    List<CategoryResponse>> getMenu() {
        return ResponseEntity.ok(categoryService.getActiveForMenu());
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponse>> search(@RequestParam(required = false) String keyword,
    @RequestParam(required = false) Boolean status) {
    return ResponseEntity.ok(categoryService.searchByName(keyword, status));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(@RequestParam(required = false) Boolean status) {
        return ResponseEntity.ok(categoryService.getAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa danh mục thành công"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }
 
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
 
    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}