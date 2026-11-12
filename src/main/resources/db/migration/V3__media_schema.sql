-- Nhóm media: thông tin file đã tải lên và việc gắn file vào bản ghi.

CREATE TABLE med_file (
    workspace_id    BIGINT       NOT NULL,
    id              BIGINT       NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    width           INT          NULL,
    height          INT          NULL,
    checksum_sha256 CHAR(64)     CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    uploaded_by     BIGINT       NOT NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, id),
    KEY idx_med_file_checksum (workspace_id, checksum_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- owner_id trỏ sang bảng của nhóm registry nên không có khoá ngoại.
CREATE TABLE med_attachment (
    workspace_id BIGINT       NOT NULL,
    id           BIGINT       NOT NULL,
    owner_type   VARCHAR(20)  NOT NULL,
    owner_id     BIGINT       NOT NULL,
    file_id      BIGINT       NOT NULL,
    caption      VARCHAR(255) NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    added_by     BIGINT       NOT NULL,
    added_at     DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    removed_by   BIGINT       NULL,
    removed_at   DATETIME(3)  NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_med_attachment_owner (workspace_id, owner_type, owner_id, removed_at, sort_order),
    KEY idx_med_attachment_file (workspace_id, file_id),
    CONSTRAINT fk_med_attachment_file FOREIGN KEY (workspace_id, file_id) REFERENCES med_file (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
