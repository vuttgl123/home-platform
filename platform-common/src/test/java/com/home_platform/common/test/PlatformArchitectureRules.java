package com.home_platform.common.test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.List;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quy ước của dự án viết thành luật ArchUnit, để vi phạm lộ ra ở {@code ./mvnw test} thay vì
 * lúc review. Mỗi service có một test ngắn gọi {@link #check(String)} với package của nó.
 *
 * <p>Repository là lớp có {@code @Repository}, controller là lớp có {@code @RestController}.
 * Luật không báo lỗi khi service chưa có lớp nào thuộc loại đó.
 */
public final class PlatformArchitectureRules {

	/** Truy vấn bằng {@code JdbcClient}; không JPA, không Hibernate ORM (Hibernate Validator vẫn được). */
	public static final ArchRule NO_JPA = noClasses().should()
		.dependOnClassesThat(resideInAPackage("jakarta.persistence.."))
		.orShould()
		.dependOnClassesThat(resideInAPackage("org.hibernate..").and(not(resideInAPackage("org.hibernate.validator.."))))
		.because("data access uses JdbcClient with hand-written SQL")
		.allowEmptyShould(true);

	/**
	 * Mọi hàm public của repository nhận {@code workspaceId} (kiểu {@code long}) hoặc
	 * {@link WorkspaceContext} làm tham số đầu tiên, nên không truy vấn nào quên lọc theo không gian.
	 */
	public static final ArchRule REPOSITORY_METHODS_START_WITH_WORKSPACE = methods().that()
		.areDeclaredInClassesThat()
		.areAssignableTo(WorkspaceRepository.class)
		.and()
		.areDeclaredInClassesThat()
		.doNotHaveFullyQualifiedName(WorkspaceRepository.class.getName())
		.and()
		.arePublic()
		.should(takeWorkspaceFirst())
		.allowEmptyShould(true);

	/** Chỉ repository chạy SQL. Lớp lồng bên trong một repository được tính là của repository đó. */
	public static final ArchRule ONLY_REPOSITORIES_USE_JDBC = noClasses().that(not(insideARepository()))
		.should()
		.dependOnClassesThat(belongToAnyOf(JdbcClient.class, JdbcTemplate.class, NamedParameterJdbcTemplate.class))
		.because("only @Repository classes run SQL")
		.allowEmptyShould(true);

	/** Controller đi qua service, không gọi thẳng repository. */
	public static final ArchRule CONTROLLERS_DO_NOT_USE_REPOSITORIES = noClasses().that()
		.areMetaAnnotatedWith(RestController.class)
		.should()
		.dependOnClassesThat()
		.areMetaAnnotatedWith(Repository.class)
		.because("controllers call services, services call repositories")
		.allowEmptyShould(true);

	private PlatformArchitectureRules() {
	}

	/**
	 * Lớp của service không dùng lớp của service khác; ngoài chính nó chỉ được dùng
	 * {@code com.home_platform.common}.
	 */
	public static ArchRule noOtherServices(String servicePackage) {
		return noClasses().that()
			.resideInAPackage(servicePackage + "..")
			.should()
			.dependOnClassesThat(resideInAPackage("com.home_platform..")
				.and(resideOutsideOfPackages(servicePackage + "..", "com.home_platform.common..")))
			.because("services talk to each other over HTTP, not through each other's classes")
			.allowEmptyShould(true);
	}

	/**
	 * Nạp các lớp main của package (bỏ lớp test) và kiểm tra cả năm luật. Mọi vi phạm của mọi
	 * luật được báo cùng một lần.
	 */
	public static void check(String servicePackage) {
		JavaClasses classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages(servicePackage);
		CompositeArchRule.of(List.of(NO_JPA, noOtherServices(servicePackage), REPOSITORY_METHODS_START_WITH_WORKSPACE,
				ONLY_REPOSITORIES_USE_JDBC, CONTROLLERS_DO_NOT_USE_REPOSITORIES))
			.check(classes);
	}

	private static ArchCondition<JavaMethod> takeWorkspaceFirst() {
		return new ArchCondition<>("take long workspaceId or WorkspaceContext as first parameter") {
			@Override
			public void check(JavaMethod method, ConditionEvents events) {
				List<JavaClass> parameters = method.getRawParameterTypes();
				boolean ok = !parameters.isEmpty() && (parameters.get(0).isEquivalentTo(long.class)
						|| parameters.get(0).isEquivalentTo(WorkspaceContext.class));
				if (!ok) {
					events.add(SimpleConditionEvent.violated(method,
							method.getFullName() + " does not take workspaceId or WorkspaceContext first"));
				}
			}
		};
	}

	/** Lớp có {@code @Repository}, hoặc nằm lồng (ở bất kỳ cấp nào) trong một lớp như vậy. */
	private static DescribedPredicate<JavaClass> insideARepository() {
		return new DescribedPredicate<>("are inside a @Repository class") {
			@Override
			public boolean test(JavaClass javaClass) {
				JavaClass current = javaClass;
				while (current != null) {
					if (current.isMetaAnnotatedWith(Repository.class)) {
						return true;
					}
					current = current.getEnclosingClass().orElse(null);
				}
				return false;
			}
		};
	}

}
