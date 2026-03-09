package Nhom1.Demo_Nhom1.exception;

public class AdminCannotAddToCartException extends RuntimeException {
    public AdminCannotAddToCartException() {
        super("Quản trị viên không được phép thêm sản phẩm vào giỏ hàng. Vui lòng sử dụng tài khoản khách hàng.");
    }
    
    public AdminCannotAddToCartException(String message) {
        super(message);
    }
}
