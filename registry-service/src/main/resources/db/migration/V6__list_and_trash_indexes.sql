-- Đồ của cả không gian lọc theo trạng thái, và thùng rác (docs/superpowers/specs/2026-11-16-find-and-recover-design.md mục 5).
ALTER TABLE reg_item   ADD KEY idx_reg_item_status (workspace_id, status, name),
                       ADD KEY idx_reg_item_deleted (workspace_id, deleted_at);
ALTER TABLE reg_place  ADD KEY idx_reg_place_deleted (workspace_id, deleted_at);
ALTER TABLE reg_person ADD KEY idx_reg_person_deleted (workspace_id, deleted_at);
