-- Mã tự cấp (docs/superpowers/specs/2026-11-16-auto-codes-design.md).

-- Mẫu mã của một không gian: chung cho một đích (type_id 0) hoặc riêng cho một loại địa điểm, nhóm đồ.
CREATE TABLE reg_code_rule (
    workspace_id BIGINT      NOT NULL,
    target       VARCHAR(10) NOT NULL,
    type_id      BIGINT      NOT NULL DEFAULT 0,
    prefix       VARCHAR(10) CHARACTER SET ascii NOT NULL,
    digits       TINYINT     NOT NULL,
    yearly       TINYINT(1)  NOT NULL DEFAULT 0,
    created_at   DATETIME(3) NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at   DATETIME(3) NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, target, type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Số cuối đã cấp của mỗi dãy (phần đầu của mã, tới hết gạch nối cuối: "P-", "TS-2026-").
-- Chỉ tăng trong transaction của lệnh ghi mã đó, nên lệnh hỏng thì số được trả lại.
CREATE TABLE reg_code_counter (
    workspace_id BIGINT      NOT NULL,
    target       VARCHAR(10) NOT NULL,
    series       VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    last_no      BIGINT      NOT NULL,
    PRIMARY KEY (workspace_id, target, series)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mã gõ tay đang trùng: mỗi nhóm (workspace_id, code) giữ mã cho dòng chưa xoá có id nhỏ nhất
-- (cả nhóm đã xoá thì dòng có id nhỏ nhất); các dòng còn lại bỏ trống mã và được cấp mã mới sau.
-- Mã cũ của chúng vẫn xem được trong các version trước.
UPDATE reg_place p
JOIN (SELECT workspace_id, id,
             ROW_NUMBER() OVER (PARTITION BY workspace_id, code ORDER BY deleted_at IS NOT NULL, id) AS rn
      FROM reg_place WHERE code IS NOT NULL) r
  ON r.workspace_id = p.workspace_id AND r.id = p.id
SET p.code = NULL
WHERE r.rn > 1;

UPDATE reg_item i
JOIN (SELECT workspace_id, id,
             ROW_NUMBER() OVER (PARTITION BY workspace_id, code ORDER BY deleted_at IS NOT NULL, id) AS rn
      FROM reg_item WHERE code IS NOT NULL) r
  ON r.workspace_id = i.workspace_id AND r.id = i.id
SET i.code = NULL
WHERE r.rn > 1;

-- Mã không trùng trong một không gian, kể cả với dòng đã xoá: mã đã cấp không bao giờ dùng lại.
ALTER TABLE reg_place DROP KEY idx_reg_place_code, ADD UNIQUE KEY uk_reg_place_code (workspace_id, code);
ALTER TABLE reg_item DROP KEY idx_reg_item_code, ADD UNIQUE KEY uk_reg_item_code (workspace_id, code);
