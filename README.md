# SEO Analyzer

Hệ thống phân tích và đánh giá SEO on-page ứng dụng trí tuệ nhân tạo. Người dùng nhập địa chỉ một trang web, hệ thống thu thập dữ liệu, chấm điểm theo 8 tiêu chí có căn cứ từ tài liệu chính thức của Google, đo tốc độ tải thật, rồi dùng AI đề xuất cách sửa cụ thể.

Đồ án tốt nghiệp ngành Công nghệ thông tin, Trường Đại học Thuỷ Lợi.

![Trang kết quả phân tích](docs/ket-qua.png)

## Chức năng

- **Phân tích SEO on-page** từ một URL: thẻ title, meta description, heading, thuộc tính alt của ảnh, thẻ canonical, liên kết nội bộ.
- **Đo Core Web Vitals** (LCP, INP, CLS) qua Google PageSpeed Insights, ưu tiên dữ liệu người dùng thật.
- **Chấm điểm thang 100** theo trọng số từng tiêu chí, kèm nhận xét và căn cứ lý thuyết.
- **Mô phỏng hiển thị trên Google**: cho thấy title và meta description bị cắt ở đâu trên trang kết quả tìm kiếm.
- **Gợi ý cải thiện bằng AI** (Gemini), có công cụ đếm lại độ dài bản viết lại.
- **Lưu lịch sử** mọi lần phân tích, vẽ biểu đồ điểm của một trang theo thời gian.

## Bộ tiêu chí chấm điểm

| # | Tiêu chí | Trọng số | Điều kiện đạt | Căn cứ |
|---|---|---|---|---|
| 1 | Thẻ title | 20 | Có, dài 30–60 ký tự | Google SEO Starter Guide; ngưỡng độ dài: Moz |
| 2 | Meta description | 15 | Có, dài 70–160 ký tự | Google SEO Starter Guide; ngưỡng độ dài: Moz |
| 3 | Thẻ H1 | 15 | Có ít nhất 1 H1 có nội dung | Google SEO Starter Guide |
| 4 | Phân cấp heading | 10 | Có H2, không nhảy cấp từ H1 xuống H3 | Thực tiễn ngành |
| 5 | Alt của ảnh | 15 | Từ 90% ảnh có alt | Google Image SEO Guidelines |
| 6 | Thẻ canonical | 10 | Có URL canonical hợp lệ | Google Search Central |
| 7 | Liên kết nội bộ | 15 | Có ít nhất 1 liên kết nội bộ | Google SEO Starter Guide |
| 8 | Core Web Vitals | 15 | LCP ≤ 2,5 s; INP ≤ 200 ms; CLS ≤ 0,1 | Google Core Web Vitals (web.dev) |

Mỗi tiêu chí được chấm 1 (đạt), 0,5 (đạt một phần) hoặc 0 (chưa đạt), nhân với trọng số rồi quy về thang 100. Nếu không đo được Core Web Vitals, tiêu chí 8 không được tính vào điểm.

## Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 25 |
| Framework | Spring Boot 4.1 (Spring MVC, Spring Data JPA) |
| Giao diện | Thymeleaf (render phía server), CSS thuần, biểu đồ SVG vẽ trên server |
| Cơ sở dữ liệu | MySQL 8.4 |
| Thu thập dữ liệu | Jsoup |
| Đo tốc độ | Google PageSpeed Insights API v5 |
| AI | Google Gemini API (model `gemini-3.8-flash`) |

## Cài đặt và chạy

### Yêu cầu

- JDK 25
- MySQL 8.4 đang chạy ở cổng 3306
- Gemini API key và PageSpeed Insights API key (đều có gói miễn phí)

### Các bước

1. Tải mã nguồn:
   ```bash
   git clone https://github.com/1stdung802/SEO.git
   cd SEO
   ```
2. Tạo file cấu hình bí mật từ file mẫu:
   ```bash
   cp src/main/resources/secret.properties.example src/main/resources/secret.properties
   ```
   Mở `secret.properties` và điền mật khẩu MySQL cùng 2 API key.
3. Chạy ứng dụng. Database `seo_analyzer` và các bảng được tạo tự động ở lần chạy đầu.
   ```bash
   ./mvnw spring-boot:run        # macOS / Linux
   .\mvnw.cmd spring-boot:run    # Windows
   ```
4. Mở trình duyệt tại `http://localhost:8080`.

## Địa chỉ trong ứng dụng

### Giao diện

| Địa chỉ | Nội dung |
|---|---|
| `/` | Trang chủ: nhập URL, các lần phân tích gần đây |
| `/analysis/{id}` | Kết quả một lần phân tích |
| `/history` | Lịch sử, lọc theo URL |

### API (trả về JSON)

| Phương thức | Địa chỉ | Mô tả |
|---|---|---|
| GET | `/api/analyze?url=...` | Phân tích và lưu kết quả |
| GET | `/api/history?url=...` | Lịch sử phân tích (bỏ `url` để xem tất cả) |
| GET | `/api/history/{id}` | Chi tiết một lần phân tích |
| GET | `/api/history/{id}/suggestions` | Gợi ý cải thiện bằng AI (lưu lại sau lần đầu) |
| GET | `/api/crawl?url=...` | Chỉ trích xuất dữ liệu thô, không chấm điểm |

## Cấu trúc mã nguồn

```
src/main/java/com/dung/seoanalyzer/seoanalyzer/
├── controller/   Nhận yêu cầu: trang giao diện và API JSON
├── service/      Nghiệp vụ: thu thập, đo tốc độ, chấm điểm, gọi AI, chuẩn bị dữ liệu hiển thị
├── criteria/     Mỗi tiêu chí SEO là một class riêng, cùng tuân theo interface SeoCriterion
├── entity/       Ánh xạ bảng analyses và analysis_criteria
├── repository/   Truy vấn database bằng Spring Data JPA
└── dto/          Đối tượng truyền dữ liệu giữa các tầng
```

Thêm tiêu chí mới chỉ cần tạo một class mới trong `criteria/`. Spring tự động đưa class đó vào quy trình chấm điểm mà không phải sửa code cũ.

## Hạn chế

- Jsoup chỉ đọc HTML gốc, không chạy JavaScript, nên nội dung tải động (ảnh lazy-load, trang dựng hoàn toàn bằng JavaScript) có thể bị đếm thiếu.
- Số từ được đếm theo khoảng trắng, nên với tiếng Việt là đếm theo âm tiết chứ không theo từ.
- AI có thể ghi sai số ký tự của bản viết lại. Giao diện có công cụ đếm lại để kiểm chứng.
- Ở gói miễn phí, dữ liệu gửi lên Gemini có thể được Google dùng để cải thiện sản phẩm. Hệ thống chỉ gửi dữ liệu công khai của trang web được phân tích.

## Tác giả

Nguyễn Quang Dũng, lớp 64CNTT3, Trường Đại học Thuỷ Lợi.
Giáo viên hướng dẫn: ThS. Hoàng Quốc Dũng.
