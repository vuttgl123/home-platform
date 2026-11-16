-- Chủ của món đồ (docs/superpowers/specs/2026-11-16-ownership-design.md mục 5.5): trống là món của
-- địa điểm, có giá trị là tài sản riêng của người đó. Đổi bằng thao tác riêng, có version.
ALTER TABLE reg_item
    ADD COLUMN owner_person_id BIGINT NULL AFTER replaced_by_item_id,
    ADD KEY idx_reg_item_owner (workspace_id, owner_person_id, removed_at, name),
    ADD CONSTRAINT fk_reg_item_owner FOREIGN KEY (workspace_id, owner_person_id)
        REFERENCES reg_person (workspace_id, id);
