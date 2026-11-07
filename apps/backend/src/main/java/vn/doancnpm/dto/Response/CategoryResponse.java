package vn.doancnpm.dto.Response;

public class CategoryResponse {

    private Long id;
    private String name;
    private Boolean status;
    private Long displayOrder;

    public CategoryResponse() {
    }

    public CategoryResponse(Long id, String name, Boolean status, Long displayOrder) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Long getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Long displayOrder) {
        this.displayOrder = displayOrder;
    }
}
