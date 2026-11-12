-- Nhóm registry: loại địa điểm, địa điểm, địa chỉ, người, việc cư trú, đồ đạc, version.

CREATE TABLE reg_place_type (
    workspace_id BIGINT       NOT NULL,
    id           BIGINT       NOT NULL,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500) NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, id),
    UNIQUE KEY uk_reg_place_type_code (workspace_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_field_def (
    workspace_id BIGINT       NOT NULL,
    id           BIGINT       NOT NULL,
    target       VARCHAR(10)  NOT NULL,
    type_id      BIGINT       NOT NULL DEFAULT 0,
    field_key    VARCHAR(50)  NOT NULL,
    label        VARCHAR(100) NOT NULL,
    data_type    VARCHAR(20)  NOT NULL,
    options      JSON         NULL,
    is_required  TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order   INT          NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, id),
    UNIQUE KEY uk_reg_field_def_key (workspace_id, target, type_id, field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_place (
    workspace_id  BIGINT       NOT NULL,
    id            BIGINT       NOT NULL,
    parent_id     BIGINT       NULL,
    place_type_id BIGINT       NOT NULL,
    path          VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    depth         TINYINT      NOT NULL DEFAULT 0,
    code          VARCHAR(50)  NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT         NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    attributes    JSON         NULL,
    cover_file_id BIGINT       NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    created_by    BIGINT       NOT NULL,
    updated_at    DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_by    BIGINT       NOT NULL,
    version       INT          NOT NULL DEFAULT 1,
    deleted_at    DATETIME(3)  NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_reg_place_parent (workspace_id, parent_id, name),
    KEY idx_reg_place_path (workspace_id, path),
    KEY idx_reg_place_type (workspace_id, place_type_id, name),
    KEY idx_reg_place_name (workspace_id, name),
    KEY idx_reg_place_code (workspace_id, code),
    CONSTRAINT fk_reg_place_parent FOREIGN KEY (workspace_id, parent_id) REFERENCES reg_place (workspace_id, id),
    CONSTRAINT fk_reg_place_type FOREIGN KEY (workspace_id, place_type_id) REFERENCES reg_place_type (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_place_address (
    workspace_id  BIGINT       NOT NULL,
    place_id      BIGINT       NOT NULL,
    country_code  CHAR(2)      NOT NULL DEFAULT 'VN',
    province_code VARCHAR(10)  NULL,
    province_name VARCHAR(100) NULL,
    district_code VARCHAR(10)  NULL,
    district_name VARCHAR(100) NULL,
    ward_code     VARCHAR(10)  NULL,
    ward_name     VARCHAR(100) NULL,
    street        VARCHAR(255) NULL,
    full_address  VARCHAR(500) NOT NULL,
    latitude      DECIMAL(9,6) NULL,
    longitude     DECIMAL(9,6) NULL,
    geohash       VARCHAR(12)  CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at    DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, place_id),
    KEY idx_reg_address_area (workspace_id, province_code, ward_code),
    KEY idx_reg_address_geohash (workspace_id, geohash),
    CONSTRAINT fk_reg_address_place FOREIGN KEY (workspace_id, place_id) REFERENCES reg_place (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_person (
    workspace_id   BIGINT       NOT NULL,
    id             BIGINT       NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    phone          VARCHAR(20)  NULL,
    email          VARCHAR(255) NULL,
    id_type        VARCHAR(20)  NULL,
    id_number      VARCHAR(30)  NULL,
    date_of_birth  DATE         NULL,
    gender         VARCHAR(10)  NULL,
    note           TEXT         NULL,
    attributes     JSON         NULL,
    avatar_file_id BIGINT       NULL,
    created_at     DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    created_by     BIGINT       NOT NULL,
    updated_at     DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_by     BIGINT       NOT NULL,
    version        INT          NOT NULL DEFAULT 1,
    deleted_at     DATETIME(3)  NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_reg_person_name (workspace_id, full_name),
    KEY idx_reg_person_phone (workspace_id, phone),
    KEY idx_reg_person_id_number (workspace_id, id_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_occupancy (
    workspace_id BIGINT       NOT NULL,
    id           BIGINT       NOT NULL,
    place_id     BIGINT       NOT NULL,
    person_id    BIGINT       NOT NULL,
    relation     VARCHAR(20)  NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NULL,
    note         VARCHAR(500) NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    created_by   BIGINT       NOT NULL,
    updated_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_by   BIGINT       NOT NULL,
    version      INT          NOT NULL DEFAULT 1,
    deleted_at   DATETIME(3)  NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_reg_occupancy_place (workspace_id, place_id, end_date, start_date),
    KEY idx_reg_occupancy_person (workspace_id, person_id, start_date),
    CONSTRAINT fk_reg_occupancy_place FOREIGN KEY (workspace_id, place_id) REFERENCES reg_place (workspace_id, id),
    CONSTRAINT fk_reg_occupancy_person FOREIGN KEY (workspace_id, person_id) REFERENCES reg_person (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_item_category (
    workspace_id BIGINT       NOT NULL,
    id           BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, id),
    UNIQUE KEY uk_reg_item_category_name (workspace_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reg_item (
    workspace_id        BIGINT       NOT NULL,
    id                  BIGINT       NOT NULL,
    place_id            BIGINT       NOT NULL,
    category_id         BIGINT       NULL,
    code                VARCHAR(50)  NULL,
    name                VARCHAR(255) NOT NULL,
    quantity            INT          NOT NULL DEFAULT 1,
    unit                VARCHAR(20)  NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'IN_USE',
    placed_at           DATE         NULL,
    removed_at          DATE         NULL,
    replaced_by_item_id BIGINT       NULL,
    description         TEXT         NULL,
    attributes          JSON         NULL,
    cover_file_id       BIGINT       NULL,
    created_at          DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    created_by          BIGINT       NOT NULL,
    updated_at          DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_by          BIGINT       NOT NULL,
    version             INT          NOT NULL DEFAULT 1,
    deleted_at          DATETIME(3)  NULL,
    PRIMARY KEY (workspace_id, id),
    KEY idx_reg_item_place (workspace_id, place_id, removed_at, name),
    KEY idx_reg_item_category (workspace_id, category_id, name),
    KEY idx_reg_item_name (workspace_id, name),
    KEY idx_reg_item_code (workspace_id, code),
    KEY idx_reg_item_replaced_by (workspace_id, replaced_by_item_id),
    CONSTRAINT fk_reg_item_place FOREIGN KEY (workspace_id, place_id) REFERENCES reg_place (workspace_id, id),
    CONSTRAINT fk_reg_item_category FOREIGN KEY (workspace_id, category_id) REFERENCES reg_item_category (workspace_id, id),
    CONSTRAINT fk_reg_item_replaced_by FOREIGN KEY (workspace_id, replaced_by_item_id) REFERENCES reg_item (workspace_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Không có khoá ngoại: version phải sống lâu hơn bản ghi gốc, và bảng chia phần không hỗ trợ khoá ngoại.
CREATE TABLE reg_entity_version (
    workspace_id   BIGINT       NOT NULL,
    entity_type    VARCHAR(20)  NOT NULL,
    entity_id      BIGINT       NOT NULL,
    version_no     INT          NOT NULL,
    action         VARCHAR(10)  NOT NULL,
    snapshot       JSON         NOT NULL,
    changed_fields JSON         NULL,
    changed_by     BIGINT       NOT NULL,
    changed_at     DATETIME(3)  NOT NULL,
    note           VARCHAR(255) NULL,
    PRIMARY KEY (workspace_id, entity_type, entity_id, version_no),
    KEY idx_reg_version_changed_at (workspace_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  PARTITION BY KEY (workspace_id) PARTITIONS 64;
