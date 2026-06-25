package de.tum.devopss26.shared.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public abstract class AbstractArchitectureTest {

    protected abstract JavaClasses getClasses();

    @Test
    @DisplayName("Services must not depend on Controllers")
    void servicesMustNotDependOnControllers() {
        BaseArchitectureRules.SERVICES_MUST_NOT_DEPEND_ON_CONTROLLERS.check(getClasses());
    }

    @Test
    @DisplayName("Repositories must not depend on Services")
    void repositoriesMustNotDependOnServices() {
        BaseArchitectureRules.REPOSITORIES_MUST_NOT_DEPEND_ON_SERVICES.check(getClasses());
    }

    @Test
    @DisplayName("Controllers must reside in the controller package")
    void controllersMustResideInControllerPackage() {
        BaseArchitectureRules.CONTROLLERS_MUST_RESIDE_IN_CONTROLLER_PACKAGE.check(getClasses());
    }

    @Test
    @DisplayName("Services must reside in the service package")
    void servicesMustResideInServicePackage() {
        BaseArchitectureRules.SERVICES_MUST_RESIDE_IN_SERVICE_PACKAGE.check(getClasses());
    }

    @Test
    @DisplayName("Web slice tests must have a name ending with 'ControllerTest'")
    void webSliceTestsShouldHaveCorrectNaming() {
        BaseArchitectureRules.WEB_SLICE_TESTS_SHOULD_HAVE_CORRECT_NAMING.check(getClasses());
    }

    @Test
    @DisplayName("Integration tests must have a name ending with 'IT'")
    void integrationTestsShouldHaveCorrectNaming() {
        BaseArchitectureRules.INTEGRATION_TESTS_SHOULD_HAVE_CORRECT_NAMING.check(getClasses());
    }

    @Test
    @DisplayName("Entities must reside in the entity package")
    void entitiesMustResideInEntityPackage() {
        BaseArchitectureRules.ENTITIES_MUST_RESIDE_IN_ENTITY_PACKAGE.check(getClasses());
    }

    @Test
    @DisplayName("Controllers must implement OpenAPI API interfaces")
    void controllersMustImplementApiInterfaces() {
        BaseArchitectureRules.CONTROLLERS_MUST_IMPLEMENT_API_INTERFACES.check(getClasses());
    }
}
