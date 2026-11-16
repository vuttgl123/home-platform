-- Sở hữu địa điểm (docs/superpowers/specs/2026-11-16-ownership-design.md): người X sở hữu
-- share_percent phần trăm của địa điểm Y từ start_date tới end_date (trống là đang sở hữu).
CREATE TABLE reg_ownership (
    workspace_id  BIGINT        NOT NULL,
    id            BIGINT        NOT NULL,
    place_id      BIGINT        NOT NULL,
    person_id     BIGINT        NOT NULL,
    share_percent DECIMAL(5,2)  NULL,
    start_date    DATE          NOT NULL,
    end_date      DATE          NULL,
    document_no   VARCHAR(100)  NULL,
    note          VARCHAR(500)  NULL,
    created_at    DATETIME(3)   NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    created_by    BIGINT        NOT NULL,
    updated_at    DATETIME(3)   NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_by    BIGINT        NOT NULL,
    version       INT           NOT NULL DEFAULT 1,
    deleted_at    DATETIME(3)   NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_reg_ownership_place (workspace_id, place_id, end_date, start_date),
    KEY idx_reg_ownership_person (workspace_id, person_id, start_date),
    CONSTRAINT fk_reg_ownership_place FOREIGN KEY (workspace_id, place_id) REFERENCES reg_place (workspace_id, id),
    CONSTRAINT fk_reg_ownership_person FOREIGN KEY (workspace_id, person_id) REFERENCES reg_person (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng sở hữu là nơi duy nhất ghi chủ: chép các dòng cư trú "Chủ" chưa xoá sang (dùng lại id; hai
-- bảng có khoá riêng). Dòng cư trú giữ nguyên; người dùng tự dọn.
INSERT INTO reg_ownership (workspace_id, id, place_id, person_id, start_date, end_date, note,
                           created_at, created_by, updated_at, updated_by, version)
SELECT workspace_id, id, place_id, person_id, start_date, end_date, note,
       created_at, created_by, updated_at, updated_by, 1
FROM reg_occupancy WHERE relation = 'OWNER' AND deleted_at IS NULL;
