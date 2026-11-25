-- Bảo trì và nhắc việc (docs/superpowers/specs/2026-11-25-maintenance-and-reminders-design.md).

-- Nhật ký bảo trì của một món đồ: mỗi dòng là một lần sửa (REPAIR) hay bảo trì định kỳ (SERVICE), có
-- version riêng (entity_type MAINTENANCE). Xoá món không xoá nhật ký; các dòng ẩn theo món.
CREATE TABLE reg_maintenance (
    workspace_id BIGINT        NOT NULL,
    id           BIGINT        NOT NULL,
    item_id      BIGINT        NOT NULL,
    kind         VARCHAR(10)   NOT NULL,
    done_on      DATE          NOT NULL,
    work         VARCHAR(500)  NOT NULL,
    done_by      VARCHAR(150)  NULL,
    note         VARCHAR(1000) NULL,
    created_at   DATETIME(3)   NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    created_by   BIGINT        NOT NULL,
    updated_at   DATETIME(3)   NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_by   BIGINT        NOT NULL,
    version      INT           NOT NULL DEFAULT 1,
    deleted_at   DATETIME(3)   NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_reg_maintenance_item (workspace_id, item_id, done_on),
    CONSTRAINT fk_reg_maintenance_item FOREIGN KEY (workspace_id, item_id) REFERENCES reg_item (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ngày cuối cùng còn bảo hành, và chu kỳ bảo trì (1 tới 120 tháng); bỏ trống là chưa đặt (L3).
ALTER TABLE reg_item
    ADD COLUMN warranty_until       DATE             NULL AFTER placed_at,
    ADD COLUMN service_every_months TINYINT UNSIGNED NULL AFTER warranty_until,
    ADD KEY idx_reg_item_warranty (workspace_id, warranty_until);

-- Mục kiểu ngày nhắc trong Việc cần làm trước remind_days ngày; bỏ trống là không nhắc (L4).
ALTER TABLE reg_field_def ADD COLUMN remind_days SMALLINT UNSIGNED NULL AFTER is_required;

-- Lần cuối mỗi người mở chuông trong một không gian: việc có since sau mốc này là việc mới (L6).
CREATE TABLE reg_todo_seen (
    workspace_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    seen_at      DATETIME(3) NOT NULL,
    PRIMARY KEY (workspace_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
