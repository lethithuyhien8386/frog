# Ghi chú phân tích 11 video — Froggy Pilot

**Nguồn:** 11 tệp MP4 do người dùng gửi trong cuộc trò chuyện này, mỗi tệp dài khoảng 9–37 phút. Ảnh kiểm thử trích từ thời điểm liệt kê dưới đây và lưu tại `tests/frames` (co về 360×640). Việc trích các thời điểm đại diện không có nghĩa là đã xác minh bằng tay mọi lượt thao tác trong toàn bộ video.

| Video | Thời điểm lấy ảnh | Giao diện quan sát được / phương án nhận diện |
|---|---:|---|
| `1(1).mp4` | 03:10 | Quán và các mục nâng cấp dưới màn hình, tìm nút mũi tên xanh. |
| `2(1).mp4` | 10:00 | Màn quán, thanh nâng cấp; xử lý bình thường, không giả định cố định chỗ đặt bàn/món. |
| `3(1).mp4` | 08:40 | Hộp **Level 10 – Waffle**, nút lớn màu xanh có biểu tượng đồng xu; ưu tiên nút giá xu. |
| `4(1).mp4` | 05:40 | Hộp **Manage Eatery** với các mục **Quick hands** và **The more the merrier**, giá xu màu xanh; phía trên có **Froggy Bank** và nút Open/gem riêng, không nhấn tùy tiện. |
| `5(1).mp4` | 23:20 | Quán nhiều vị trí, máy/món ăn và đơn hàng; không nhầm các biểu tượng ở quán với hộp thoại nổi. |
| `6.mp4` | 17:54 | Quán nhiều nhân vật ếch và biểu tượng đồ ăn; chọn điều khiển nâng cấp ở chân màn hình thay vì chạm ngẫu nhiên các đơn hàng. |
| `7.mp4` | 03:27 | Bố cục quán khác; tìm nút điều khiển tại thanh dưới cùng. |
| `8.mp4` | 07:21 | Quán có các khu vực làm món và món chờ; không giả định mọi ô vuông là nút mua. |
| `9.mp4` | 15:00 | Cửa sổ trang bị **Hetman Bulawa**, nút **Refund** màu xanh và nút **UPGRADE** dùng kim cương; chọn **đóng** cửa sổ, không mua gem hoặc nhầm Refund là tiếp tục. |
| `10.mp4` | 14:40 | Quán lớn với nhiều máy chế biến, món hàng và thanh nâng cấp; tiếp tục quan sát, mở nâng cấp định kỳ. |
| `11.mp4` | 04:34 | Hộp **Manage Eatery** với các nâng cấp bằng xu, **Froggy Bank is full!** kèm Open/gem; ưu tiên nút giá xu trong danh sách. |

## Luồng xử lý trong ứng dụng

1. Chờ người dùng chọn Start. Lấy ảnh bằng API Android, giảm về bề rộng 360 px; **không** gửi ảnh cho bất kỳ dịch vụ nào.
2. Tìm vùng xanh lá dạng nút ngang, kiểm tra biểu tượng giá **đồng xu vàng** nằm trong nút; không chọn nút xanh chỉ vì nó xanh.
3. Nếu là màn có nút nâng cấp bằng xu, chạm nút. Nếu nhận diện được một bảng thoại nhưng không có cách mua an toàn, có thể đóng bảng thoại ở dấu × bên góc trên. Một số nút tiếp tục lớn được nhận diện như một lựa chọn riêng.
4. Trên màn quán bình thường, định kỳ mở trang nâng cấp qua nút mũi tên xanh phía dưới (giới hạn mỗi lần khoảng 13,5 giây).
5. So sánh dấu vân tay ảnh và vị trí chạm để hạn chế lặp lại thao tác khi giao diện không đổi; Stop hủy lịch chụp / nhấn kế tiếp.

## Những việc chưa được chứng minh

- Chưa có thử nghiệm tương tác trực tiếp với game trên thiết bị Android/CloudPhone đích.
- Chưa chứng minh bot hoàn thành toàn bộ màn hoặc xử lý mọi đơn hàng, tình huống ngoại lệ, quảng cáo hay màn UI khác trong 11 video.
- Chưa mô phỏng đầy đủ luồng di chuyển nhân viên, sản xuất và giao món; game nhiều phần vận hành tự động nhưng khả năng tiến độ phụ thuộc phiên bản.
- Chưa chạy bản build APK từ Github Actions trong môi trường hiện tại; workflow được cung cấp để kiểm chứng compile, cấu trúc/chữ ký APK, cài đặt và khởi chạy trên emulator sau khi người dùng đẩy dự án lên GitHub.

Lựa chọn không tự dùng tiền thật hoặc kim cương có chủ đích, để tránh giao dịch không mong muốn.
