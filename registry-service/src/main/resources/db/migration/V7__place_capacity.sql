-- Sức chứa của một địa điểm: số người ở tối đa, bỏ trống là chưa đặt (Đợt 6, L1).
ALTER TABLE reg_place ADD COLUMN capacity SMALLINT UNSIGNED NULL AFTER status;
