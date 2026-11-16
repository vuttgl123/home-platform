package com.home_platform.registry.item;

import com.home_platform.registry.support.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Đổi chủ của một món.
 *
 * @param ownerPersonId chủ mới; trống là trả món về cho địa điểm
 */
public record ItemOwnerRequest(@NotNull Integer version, Long ownerPersonId, @Size(max = 255) String note) {

	public ItemOwnerRequest {
		note = Text.trimToNull(note);
	}

}
