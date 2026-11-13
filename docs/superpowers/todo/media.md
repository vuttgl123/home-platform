# Việc còn lại: media (ảnh)

Ngày ghi: 2026-11-13. Luật nghiệp vụ đã chốt: `docs/superpowers/specs/2026-11-13-business-rules-design.md` mục 6.

## Đã có sẵn trong `media-service`

| Lớp | Việc |
|---|---|
| `file/MediaFile`, `file/FileStatus` | Record ứng với `med_file`; test xác nhận khớp đúng các cột |
| `attachment/Attachment`, `attachment/OwnerType` | Record ứng với `med_attachment`; test xác nhận khớp đúng các cột |
| `file/UploadPolicy` | Chỉ nhận JPEG, PNG, WebP; tối đa 10 MB (có test) |
| `storage/FileStorage`, `storage/StoredObject` | Interface kho file; **chưa có bản cài đặt, chưa đăng ký bean** |
| `storage/StorageKeys` | Tên file trong kho: `workspaces/<workspaceId>/files/<fileId>` (có test) |

Trong code, mọi chỗ còn dang dở có ghi chú `TODO(media)`; tìm bằng `grep -rn "TODO(media)" media-service`.

## Cần quyết trước khi làm

1. **Kho lưu file:** thư mục trên đĩa máy chủ (chạy được ngay), MinIO tự chạy, hoặc S3/dịch vụ tương thích S3. Đề xuất: bắt đầu bằng thư mục trên đĩa, đổi sau bằng cấu hình.
2. **Cách client nhận ảnh:** service trả thẳng nội dung (`GET /files/{id}/content`), hay trả đường dẫn tạm có chữ ký (presigned URL) khi dùng S3/MinIO.
3. **Ảnh thu nhỏ (thumbnail):** có cần không, và kích thước nào.

## Thứ tự làm khi bắt đầu

1. Bản cài đặt `FileStorage` theo kho đã chọn, kèm test hợp đồng dùng chung cho mọi bản cài đặt.
2. `MediaFileRepository` (kế thừa `WorkspaceRepository<MediaFile>`) và tải lên: kiểm tra bằng `UploadPolicy`, tính SHA-256, dùng lại file READY trùng checksum, tạo dòng PENDING, ghi vào kho, đọc kích thước ảnh, chuyển READY.
3. `AttachmentRepository` (SQL tay) và các thao tác: gắn, gỡ (ghi `removed_by`, `removed_at`), đổi chú thích, sắp xếp lại, danh sách ảnh của một bản ghi.
4. Đọc nội dung file theo quyết định số 2.
5. Dọn dòng PENDING bị bỏ dở (tác vụ định kỳ).
6. README, `./mvnw verify`, chạy thử.

## Lưu ý đã biết

- Media không kiểm tra được `owner_id` có tồn tại bên registry, và registry không kiểm tra được `cover_file_id`/`avatar_file_id` bên media: hai service chưa gọi nhau.
- Xoá bản ghi bên registry không gỡ ảnh, để khôi phục bản ghi thì có lại đủ ảnh.
- Cột chuẩn của `med_attachment` mang tên khác (`added_by`, `added_at`, `removed_*`), nên repository của nó viết SQL tay thay vì kế thừa `WorkspaceRepository`.
