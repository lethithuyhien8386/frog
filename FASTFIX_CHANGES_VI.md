# FroggyPilot v1.1 FASTFIX - Vá lỗi không thao tác ở màn hướng dẫn

## Đã sửa

1. Ảnh người dùng 443×796: game làm tối màn, làm sáng ô màu trắng/xanh nhạt tại khoảng (150,335) bên dưới mũi tên đỏ. Bộ nhận diện v1.0 chỉ tìm nút nâng cấp xanh hình chữ nhật nên hiện `SCANNING` và không chạm.
2. Thêm thuật toán nhận diện đối tượng được **đánh sáng riêng** trong màn hướng dẫn (tính vị trí tự động, không chạm bừa vào tọa độ cứng).
3. Ưu tiên xử lý bước hướng dẫn trước mọi quy tắc mua nâng cấp.
4. Sau khi bấm ▶, quét đầu tiên sau khoảng 45 ms; giảm giãn cách giữa các lần quét/chạm cho bước hướng dẫn. Android có giới hạn tốc độ API screenshot nên không cam kết chính xác số mili giây trên mọi thiết bị.
5. Hiển thị `TUTORIAL` khi gửi lệnh chạm vùng hướng dẫn; `NO TARGET` nếu không nhận diện được mục tiêu an toàn.
6. Bổ sung kiểm thử hồi quy dựa trên ảnh chụp bị kẹt, khung hình tutorial trong video và các cửa sổ nâng cấp để tránh nhấn sai.
7. Giữ workflow SDKFIX v2 đã dùng setup-android v4, bộ kiểm tra APK cùng phép thử cài/khởi chạy trên emulator.

## Còn giới hạn

- Không có khả năng chứng minh tự hoàn thành tất cả màn hoặc tốc độ thực tế trên CloudPhone khi chưa chạy thử.
- Tránh tự chọn quảng cáo, mua kim cương hoặc tiền thật.
- Nếu trò chơi đổi kiểu hình ảnh hướng dẫn thì thuật toán cần ảnh mới để cập nhật.
- Đây là mã nguồn build trên GitHub; không phải APK build xong.

## Hướng dẫn

Giải nén ZIP -> đưa toàn bộ file/thư mục (kể cả `.github`) lên repo -> Actions -> Build and verify Froggy Pilot APK -> Run workflow.
