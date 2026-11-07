package vn.doancnpm.dto.Request;

public class CategoryRequest {

    private String name;
    private Boolean status;      
    private Long displayOrder;

    public CategoryRequest() {
    }

    public CategoryRequest(String name, Boolean status, Long displayOrder) {
        this.name = name;
        this.status = status;
        this.displayOrder = displayOrder;
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