package com.home_platform.common.arch;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.home_platform.common.test.PlatformArchitectureRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class PlatformArchitectureRulesTests {

	private static final String FIXTURES = "com.home_platform.archfixture.";

	@Test
	void cleanFixturePassesEveryRule() {
		JavaClasses classes = load("clean");

		assertThatCode(() -> {
			PlatformArchitectureRules.NO_JPA.check(classes);
			PlatformArchitectureRules.noOtherServices(FIXTURES + "clean").check(classes);
			PlatformArchitectureRules.REPOSITORY_METHODS_START_WITH_WORKSPACE.check(classes);
			PlatformArchitectureRules.ONLY_REPOSITORIES_USE_JDBC.check(classes);
			PlatformArchitectureRules.CONTROLLERS_DO_NOT_USE_REPOSITORIES.check(classes);
		}).doesNotThrowAnyException();
	}

	@Test
	void jpaIsCaught() {
		assertThatThrownBy(() -> PlatformArchitectureRules.NO_JPA.check(load("jpa"))).isInstanceOf(AssertionError.class);
	}

	@Test
	void dependencyOnAnotherServiceIsCaught() {
		assertThatThrownBy(() -> PlatformArchitectureRules.noOtherServices(FIXTURES + "crossa").check(load("crossa")))
			.isInstanceOf(AssertionError.class);
	}

	@Test
	void repositoryMethodWithoutWorkspaceIsCaught() {
		assertThatThrownBy(
				() -> PlatformArchitectureRules.REPOSITORY_METHODS_START_WITH_WORKSPACE.check(load("badrepo")))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("findByCode")
			.hasMessageContaining("findAllEverywhere");
	}

	@Test
	void jdbcOutsideRepositoryIsCaught() {
		assertThatThrownBy(() -> PlatformArchitectureRules.ONLY_REPOSITORIES_USE_JDBC.check(load("leaky")))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("LeakyService");
	}

	@Test
	void controllerUsingRepositoryIsCaught() {
		assertThatThrownBy(() -> PlatformArchitectureRules.CONTROLLERS_DO_NOT_USE_REPOSITORIES.check(load("leaky")))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("LeakyController");
	}

	@Test
	void emptyPackagePassesEveryRule() {
		assertThatCode(() -> PlatformArchitectureRules.check(FIXTURES + "nothing.here")).doesNotThrowAnyException();
	}

	private static JavaClasses load(String fixture) {
		return new ClassFileImporter().importPackages(FIXTURES + fixture);
	}

}
