# Sửa lỗi GitHub Actions: Failed to find package 'tools'

Log của bản ZIP cũ xuất hiện tại bước `android-actions/setup-android@v3`:

```text
sdkmanager tools
Warning: Failed to find package 'tools'
```

**Nguyên nhân gốc:** action `setup-android@v3` tự yêu cầu gói SDK `tools` theo mặc định, dù bước `sdkmanager --install` tiếp theo trong workflow không nhắc tới `tools`. Gói `tools` cũ đã bị ngừng cung cấp.

**Đã sửa trong `.github/workflows/build-apk.yml`:**

```yaml
- name: Android SDK command-line tools (no legacy tools package)
  uses: android-actions/setup-android@v4
  with:
    packages: 'platform-tools'
- name: Install Android API 35 and Build-Tools 35.0.0
  run: sdkmanager --install 'platforms;android-35' 'build-tools;35.0.0'
```

**Dùng trên GitHub:** giải nén ZIP, cập nhật *toàn bộ* nội dung vào gốc repository (nhất là file `.github/workflows/build-apk.yml`), commit & push, chạy workflow mới trong Actions. Khi tất cả job xanh, tải artifact `FroggyPilot-Auto-InstallChecked-APK`.

**Kiểm tra tại đây:** đã xác thực cấu trúc ZIP, kiểm tra cú pháp cơ bản YAML, chạy kiểm thử nhận diện Java trên 13 ảnh ví dụ; chưa chạy Gradle build hoặc giả lập Android do máy làm việc không có bộ Android SDK và Gradle. Build APK, kiểm chữ ký và kiểm thử cài/khởi chạy là các bước workflow sẽ chạy trên GitHub.

**Lưu ý:** lỗi này liên quan đến build, không chứng minh khả năng tự chơi hết các màn trong game.
