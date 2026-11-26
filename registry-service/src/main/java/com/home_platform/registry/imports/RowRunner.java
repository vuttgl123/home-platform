package com.home_platform.registry.imports;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.ErrorResponseException;

/**
 * Chạy lệnh ghi của một dòng trong một savepoint riêng: dòng hỏng chỉ quay về savepoint của nó (cờ
 * "phải huỷ" do lệnh tạo đặt cũng được xoá), các dòng sau vẫn chạy, nên một lần xem trước liệt kê
 * được mọi lỗi.
 */
@Component
class RowRunner {

	private static final Logger log = LoggerFactory.getLogger(RowRunner.class);

	private final TransactionTemplate savepoint;

	private final Validator validator;

	RowRunner(PlatformTransactionManager transactions, Validator validator) {
		this.savepoint = new TransactionTemplate(transactions);
		this.savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		this.validator = validator;
	}

	/**
	 * Kiểm request bằng các ràng buộc của nó, như controller làm với {@code @Valid}. Cột đã có lỗi
	 * (do đọc ô) thì không thêm lỗi nữa.
	 */
	void validate(Object request, RowReader reader, Map<String, String> columnOfField) {
		this.validator.validate(request).forEach(violation -> {
			RowMessage message = ImportMessages.violation(violation, columnOfField);
			if (message.column() == null || !reader.hasErrorIn(message.column())) {
				reader.addAll(java.util.List.of(message));
			}
		});
	}

	/**
	 * Chạy {@code write} khi dòng chưa có lỗi nào.
	 * @return kết quả của lệnh ghi; {@code null} khi dòng có lỗi (lỗi đã ghi vào {@code reader})
	 */
	<T> T run(RowReader reader, Map<String, String> columnOfField, Supplier<T> write) {
		if (reader.hasErrors()) {
			return null;
		}
		try {
			return this.savepoint.execute(status -> write.get());
		}
		catch (RuntimeException ex) {
			if (!(ex instanceof ErrorResponseException || ex instanceof CellError
					|| ex instanceof ConstraintViolationException)) {
				log.warn("Import row {} failed unexpectedly", reader.number(), ex);
			}
			reader.addAll(ImportMessages.of(ex, columnOfField));
			return null;
		}
	}

}
