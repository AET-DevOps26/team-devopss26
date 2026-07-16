package de.tum.devopss26.shared.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import de.tum.devopss26.shared.it.AbstractIntegrationTest;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Central collection of ArchUnit architecture rules shared across all services.
 * <p>
 * Each rule is a public static final constant that can be checked by
 * {@link AbstractArchitectureTest} subclasses.
 * </p>
 */
public class BaseArchitectureRules {

    /** Services must not have compile-time dependencies on controllers. */
    public static final ArchRule SERVICES_MUST_NOT_DEPEND_ON_CONTROLLERS =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .as("Services must not depend on Controllers")
                    .allowEmptyShould(true);

    /** Repositories must not have compile-time dependencies on services. */
    public static final ArchRule REPOSITORIES_MUST_NOT_DEPEND_ON_SERVICES =
            noClasses().that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .as("Repositories must not depend on Services")
                    .allowEmptyShould(true);

    /** Classes ending with "Controller" must reside in a controller package. */
    public static final ArchRule CONTROLLERS_MUST_RESIDE_IN_CONTROLLER_PACKAGE =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..controller..")
                    .as("Controllers must reside in the controller package")
                    .allowEmptyShould(true);

    /** Classes ending with "Service" or "ServiceImpl" must reside in a service package. */
    public static final ArchRule SERVICES_MUST_RESIDE_IN_SERVICE_PACKAGE =
            classes().that().haveSimpleNameEndingWith("ServiceImpl")
                    .or().haveSimpleNameEndingWith("Service")
                    .should().resideInAPackage("..service..")
                    .as("Services must reside in the service package")
                    .allowEmptyShould(true);

    /** Classes annotated with {@link Mapper @Mapper} must reside in a mapper package. */
    public static final ArchRule MAPPERS_MUST_RESIDE_IN_MAPPER_PACKAGE =
            classes().that().areAnnotatedWith(Mapper.class)
                    .should().resideInAPackage("..mapper..")
                    .as("Mappers must reside in the mapper package")
                    .allowEmptyShould(true);

    /** Classes that extend {@link Exception} must reside in an exception package. */
    public static final ArchRule EXCEPTIONS_MUST_RESIDE_IN_EXCEPTION_PACKAGE =
            classes().that().areAssignableTo(Exception.class)
                    .should().resideInAPackage("..exception..")
                    .as("Exceptions must reside in the exception package")
                    .allowEmptyShould(true);

    /**
     * Web slice tests annotated with {@link WebMvcTest @WebMvcTest} must have
     * a name ending with "ControllerTest".
     */
    public static final ArchRule WEB_SLICE_TESTS_SHOULD_HAVE_CORRECT_NAMING =
            classes().that().areAnnotatedWith(WebMvcTest.class)
                    .should().haveSimpleNameEndingWith("ControllerTest")
                    .as("Web slice tests (annotated with @WebMvcTest) must have a name ending with 'ControllerTest'")
                    .allowEmptyShould(true);

    /**
     * Integration tests extending {@link AbstractIntegrationTest} must have
     * a name ending with "IT".
     */
    public static final ArchRule INTEGRATION_TESTS_SHOULD_HAVE_CORRECT_NAMING =
            classes().that().areAssignableTo(AbstractIntegrationTest.class)
                    .should().haveSimpleNameEndingWith("IT")
                    .as("Integration tests (extending AbstractIntegrationTest) must have a name ending with 'IT'")
                    .allowEmptyShould(true);

    /** Classes annotated with {@code @Entity} must reside in an entity package. */
    public static final ArchRule ENTITIES_MUST_RESIDE_IN_ENTITY_PACKAGE =
            classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                    .should().resideInAPackage("..entity..")
                    .as("Entities must reside in the entity package")
                    .allowEmptyShould(true);

    /**
     * Condition that checks whether a class implements at least one interface
     * from the {@code org.openapitools.api} package.
     */
    public static final ArchCondition<JavaClass> IMPLEMENTS_AN_API_INTERFACE =
            new ArchCondition<>("implement an OpenAPI interface") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean implementsApi = false;
                    for (JavaClass interfaceClass : javaClass.getRawInterfaces()) {
                        if (interfaceClass.getPackageName().startsWith("org.openapitools.api")) {
                            implementsApi = true;
                            break;
                        }
                    }
                    if (!implementsApi) {
                        String message = String.format("Class %s does not implement any interface in package org.openapitools.api", javaClass.getName());
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    }
                }
            };

    /** Controllers must implement the corresponding OpenAPI API interface. */
    public static final ArchRule CONTROLLERS_MUST_IMPLEMENT_API_INTERFACES =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should(IMPLEMENTS_AN_API_INTERFACE)
                    .as("Controllers must implement OpenAPI API interfaces")
                    .allowEmptyShould(true);

    /**
     * Field injection (via {@link Autowired @Autowired} or {@link Value @Value})
     * is discouraged; prefer constructor or method injection.
     */
    public static final ArchRule NO_FIELD_INJECTION =
            noFields()
                    .should().beAnnotatedWith(Autowired.class)
                    .orShould().beAnnotatedWith(Value.class)
                    .as("Field injection is discouraged; use constructor or method injection instead")
                    .allowEmptyShould(true);
}
