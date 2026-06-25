package de.tum.devopss26.shared.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import de.tum.devopss26.shared.it.AbstractIntegrationTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class BaseArchitectureRules {

    public static final ArchRule SERVICES_MUST_NOT_DEPEND_ON_CONTROLLERS =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .as("Services must not depend on Controllers")
                    .allowEmptyShould(true);

    public static final ArchRule REPOSITORIES_MUST_NOT_DEPEND_ON_SERVICES =
            noClasses().that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .as("Repositories must not depend on Services")
                    .allowEmptyShould(true);

    public static final ArchRule CONTROLLERS_MUST_RESIDE_IN_CONTROLLER_PACKAGE =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..controller..")
                    .as("Controllers must reside in the controller package")
                    .allowEmptyShould(true);

    public static final ArchRule SERVICES_MUST_RESIDE_IN_SERVICE_PACKAGE =
            classes().that().haveSimpleNameEndingWith("ServiceImpl")
                    .or().haveSimpleNameEndingWith("Service")
                    .should().resideInAPackage("..service..")
                    .as("Services must reside in the service package")
                    .allowEmptyShould(true);

    public static final ArchRule WEB_SLICE_TESTS_SHOULD_HAVE_CORRECT_NAMING =
            classes().that().areAnnotatedWith(WebMvcTest.class)
                    .should().haveSimpleNameEndingWith("ControllerTest")
                    .as("Web slice tests (annotated with @WebMvcTest) must have a name ending with 'ControllerTest'")
                    .allowEmptyShould(true);

    public static final ArchRule INTEGRATION_TESTS_SHOULD_HAVE_CORRECT_NAMING =
            classes().that().areAssignableTo(AbstractIntegrationTest.class)
                    .should().haveSimpleNameEndingWith("IT")
                    .as("Integration tests (extending AbstractIntegrationTest) must have a name ending with 'IT'")
                    .allowEmptyShould(true);

    public static final ArchRule ENTITIES_MUST_RESIDE_IN_ENTITY_PACKAGE =
            classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                    .should().resideInAPackage("..entity..")
                    .as("Entities must reside in the entity package")
                    .allowEmptyShould(true);

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

    public static final ArchRule CONTROLLERS_MUST_IMPLEMENT_API_INTERFACES =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should(IMPLEMENTS_AN_API_INTERFACE)
                    .as("Controllers must implement OpenAPI API interfaces")
                    .allowEmptyShould(true);
}

