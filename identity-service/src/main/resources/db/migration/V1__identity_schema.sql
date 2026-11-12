-- Nhóm identity: người dùng, không gian, thành viên.

CREATE TABLE idn_user (
    id           BIGINT       NOT NULL,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at   DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (id),
    UNIQUE KEY uk_idn_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idn_workspace (
    id            BIGINT       NOT NULL,
    owner_user_id BIGINT       NOT NULL,
    name          VARCHAR(150) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    updated_at    DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (id),
    KEY idx_idn_workspace_owner (owner_user_id),
    CONSTRAINT fk_idn_workspace_owner FOREIGN KEY (owner_user_id) REFERENCES idn_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idn_workspace_member (
    workspace_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    role         VARCHAR(20) NOT NULL,
    joined_at    DATETIME(3) NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
    PRIMARY KEY (workspace_id, user_id),
    KEY idx_idn_member_user (user_id, workspace_id),
    CONSTRAINT fk_idn_member_workspace FOREIGN KEY (workspace_id) REFERENCES idn_workspace (id),
    CONSTRAINT fk_idn_member_user FOREIGN KEY (user_id) REFERENCES idn_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
