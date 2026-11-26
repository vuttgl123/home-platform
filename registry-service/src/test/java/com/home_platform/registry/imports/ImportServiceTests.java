package com.home_platform.registry.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ImportServiceTests {

	/**
	 * Mặc định của nền tảng là 30 giây cho mọi transaction; một file vài nghìn dòng chạy lâu hơn thế,
	 * và khi hết hạn thì mọi dòng còn lại báo lỗi dù không sai gì.
	 */
	@Test
	void importRunsInASavepointWithItsOwnLongerTimeout() {
		PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
		when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
		ImportService service = new ImportService(transactions, mock(ImportLookupRepository.class),
				mock(FieldDefService.class), mock(PlaceTypeService.class), mock(ItemCategoryService.class), List.of());

		service.preview(new WorkspaceContext(-1, -101), "x.xlsx", new byte[0]);

		ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
		verify(transactions).getTransaction(definition.capture());
		assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(definition.getValue().getTimeout()).isEqualTo(ImportService.TIMEOUT_SECONDS).isEqualTo(120);
	}

}
