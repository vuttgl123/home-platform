-- Hai người chưa xoá trong một không gian không trùng cặp (id_type, id_number). Ứng dụng vẫn kiểm
-- tra trước để báo lỗi kèm id người đã có; khoá duy nhất này chặn trường hợp hai lệnh chạy cùng lúc
-- đều qua được bước kiểm tra đó. Cột sinh ra là NULL với người đã xoá hoặc không có giấy tờ, và khoá
-- duy nhất bỏ qua NULL, nên người đã xoá không chặn ai dùng lại giấy tờ của họ.
ALTER TABLE reg_person
    ADD COLUMN live_document VARCHAR(51)
        AS (IF(deleted_at IS NULL AND id_number IS NOT NULL, CONCAT(id_type, ':', id_number), NULL)) VIRTUAL,
    ADD UNIQUE KEY uk_reg_person_live_document (workspace_id, live_document);
