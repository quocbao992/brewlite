package vn.doancnpm.service;

import vn.doancnpm.dto.Request.ToppingRequest;
import vn.doancnpm.dto.Response.ToppingResponse;
import java.util.List;
    public interface ToppingService {
   
    List<ToppingResponse> getAllToppings(Boolean status);//lấy ds topping
    ToppingResponse getToppingById(Long id); // lấy topping theo id
    ToppingResponse createTopping(ToppingRequest request);// tạo mới
    ToppingResponse updateTopping(Long id, ToppingRequest request);// cập nhập thông tin
    ToppingResponse updateStatus(Long id, boolean status);//cập nhập trạng thái
    
}
