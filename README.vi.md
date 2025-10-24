<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a>
</p>

<p align="center">
	<img src="dc3/images/logo-blue.png" width="400" alt="IoT DC3 Logo">
<br>
<a href='https://gitee.com/pnoker/iot-dc3/stargazers'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/star.svg?theme=gvp' alt='star'/>
</a>
<a href='https://gitee.com/pnoker/iot-dc3/members'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/fork.svg?theme=gvp' alt='fork'/>
</a>
<br>
<strong>
IoT DC3 là một nền tảng Internet vạn vật (IoT) phân tán hoàn toàn mã nguồn mở được xây dựng trên Spring Cloud.
Nó đẩy nhanh quá trình phát triển dự án IoT và đơn giản hóa việc quản lý thiết bị IoT, cung cấp các giải pháp toàn diện để xây dựng hệ thống IoT mạnh mẽ.
Tất cả các thành phần và mã nguồn đều là mã nguồn mở, đảm bảo tính minh bạch, linh hoạt và đổi mới do cộng đồng thúc đẩy.
</strong>
</p>

---

![iot-dc3-architecture](dc3/images/architecture-en.png)

# 1 Kiến trúc

- **Tầng Driver**: Cung cấp SDK để tạo điều kiện kết nối liền mạch với các thiết bị vật lý bằng cả giao thức tiêu chuẩn
  và độc quyền. Tầng này chịu trách nhiệm thu thập dữ liệu và
  thực thi lệnh theo hướng nam, cho phép phát triển driver nhanh chóng thông qua SDK toàn diện của nó;
- **Tầng Dữ liệu**: Quản lý việc thu thập, lưu trữ và truy xuất dữ liệu thiết bị, cung cấp dịch vụ giao diện quản lý dữ
  liệu mạnh mẽ để đảm bảo xử lý dữ liệu hiệu quả;
- **Tầng Quản lý**: Hoạt động như trung tâm cốt lõi cho các tương tác microservice, cung cấp các dịch vụ thiết yếu như
  đăng ký microservice, giao diện lệnh thiết bị, đăng ký và
  ghép nối thiết bị, và hệ thống quản lý dữ liệu tập trung. Nó giám sát các dữ liệu cấu hình khác nhau và cung cấp dịch
  vụ giao diện bên ngoài để tích hợp liền mạch;
- **Tầng Ứng dụng**: Cung cấp các chức năng nâng cao bao gồm tính mở của dữ liệu, lập lịch nhiệm vụ, thông báo cảnh báo
  và tin nhắn, quản lý nhật ký, và khả năng tích hợp với các
  nền tảng bên thứ ba, nâng cao tính đa dụng và khả năng sử dụng của nền tảng.

# 2 Mục tiêu

- **Khả năng mở rộng**: Được thiết kế để mở rộng theo chiều ngang, tận dụng sức mạnh của Spring Cloud, một công nghệ mã
  nguồn mở hàng đầu;
- **Khả năng chịu lỗi**: Đảm bảo không có điểm lỗi đơn lẻ, với mỗi nút cụm giống nhau và có thể thay thế lẫn nhau;
- **Hiệu suất**: Có khả năng xử lý hàng trăm nghìn thiết bị trên một nút máy chủ đơn, tùy thuộc vào trường hợp sử dụng
  cụ thể;
- **Khả năng tùy chỉnh**: Dễ dàng tích hợp các giao thức thiết bị mới và đăng ký chúng trong trung tâm dịch vụ;
- **Tương thích đa nền tảng**: Hoàn toàn tương thích với môi trường Java, cho phép triển khai phân tán liền mạch trên
  nhiều nền tảng;
- **Linh hoạt trong triển khai**: Hỗ trợ triển khai đám mây riêng, đám mây công cộng và edge, cung cấp quyền kiểm soát
  đầy đủ đối với cơ sở hạ tầng của bạn;
- **Hiệu quả**: Hợp lý hóa quy trình đưa thiết bị vào hệ thống, đăng ký và xác thực quyền;
- **Bảo mật**: Đảm bảo việc truyền dữ liệu được mã hóa, bảo vệ thông tin nhạy cảm;
- **Đa người thuê**: Hỗ trợ không gian tên và đa người thuê, làm cho nó lý tưởng cho các môi trường người dùng đa dạng;
- **Cloud-Native**: Được tối ưu hóa cho Kubernetes, đảm bảo tích hợp suôn sẻ với cơ sở hạ tầng đám mây hiện đại;
- **Container hóa**: Hoàn toàn được container hóa với Docker, đơn giản hóa việc triển khai và quản lý.

# 3 Phát triển

## 3.1 Phụ thuộc khởi động

> Chọn một
>
> Nếu bạn cần một tập lệnh SQL cơ sở dữ liệu, hãy kết nối trực tiếp với cơ sở dữ liệu đã khởi động trong container để
> xuất

```bash
# Truy cập toàn cầu với dịch vụ đăng ký Docker tiêu chuẩn
docker-compose -f dc3/docker-compose-db.yml up -d

# Dịch vụ đăng ký được tối ưu hóa cho người dùng ở Trung Quốc đại lục
docker-compose -f dc3/docker-compose-db-aliyun.yml up -d
```

## 3.2 Chuẩn bị

```bash
source dc3/env/dev.env.sh
mvn clean package
```

## 3.3 Khởi động dịch vụ

> Khởi động theo thứ tự

```bash
# Gateway
java -jar dc3-gateway/target/dc3-gateway.jar

# Auth Center
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar

# Data Center
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar

# Manager Center
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar

# Virtual Driver
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar

# Các driver khác: Listening Virtual Driver, Modbus TCP Driver, MQTT Driver, OPC DA Driver, OPC UA Driver, Siemens S7 Driver
```

# 4 Công nghệ sử dụng

- [Java 21](https://www.java.com)
- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.0.0](https://spring.io/projects/spring-cloud)

# 5 Đóng góp

- **Tạo nhánh**: Bắt đầu bằng cách tạo một nhánh mới từ nhánh `main`. Đảm bảo rằng nhánh `main` được cập nhật trước khi
  tạo nhánh mới;
- **Đặt tên nhánh**: Tuân theo quy ước đặt tên cho nhánh mới: `feature/your_name/feature_description`. Ví dụ:
  `feature/pnoker/mqtt_driver`;
- **Mã và Tài liệu**: Thực hiện các thay đổi của bạn đối với mã hoặc tài liệu trên nhánh mới. Sau khi hoàn thành, commit
  các thay đổi của bạn;
- **Pull Request**: Gửi một `Pull Request` (PR) để hợp nhất các thay đổi của bạn vào nhánh `develop`. PR của bạn sẽ được
  xem xét và hợp nhất bởi người bảo trì.

# 6 Giấy phép

Nền tảng mã nguồn mở `IoT DC3` được cấp phép theo [Giấy phép AGPL 3.0](./LICENSE-AGPL.txt).