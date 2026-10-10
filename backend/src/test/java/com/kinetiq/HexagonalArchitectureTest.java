package com.kinetiq;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Enforces the package rules described in backend/CLAUDE.md.
 * Layout: com.kinetiq.&lt;feature&gt;.{domain,infrastructure}, with "common" as the shared kernel.
 */
@AnalyzeClasses(packages = "com.kinetiq", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnInfrastructure = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "org.hibernate..",
                    "tools.jackson..",
                    "com.fasterxml..",
                    "org.mapstruct..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inputAdaptersDoNotUseOutputAdapters = noClasses()
            .that()
            .resideInAPackage("..infrastructure.input..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.output..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule outputAdaptersDoNotUseInputAdapters = noClasses()
            .that()
            .resideInAPackage("..infrastructure.output..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.input..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule featureInfrastructureIsPrivateToItsFeature = classes()
            .that()
            .resideInAPackage("com.kinetiq.*.infrastructure..")
            .and()
            .resideOutsideOfPackage("com.kinetiq.common..")
            .should(onlyBeUsedWithinTheirOwnFeature())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule featuresAreFreeOfCycles =
            slices().matching("com.kinetiq.(*)..").should().beFreeOfCycles();

    private static ArchCondition<JavaClass> onlyBeUsedWithinTheirOwnFeature() {
        return new ArchCondition<>("only be used within their own feature") {
            @Override
            public void check(JavaClass target, ConditionEvents events) {
                target.getDirectDependenciesToSelf().stream()
                        .filter(dependency -> !featureOf(dependency.getOriginClass()).equals(featureOf(target)))
                        .forEach(dependency ->
                                events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }
        };
    }

    /** com.kinetiq.plan.domain.model -> "plan" */
    private static String featureOf(JavaClass javaClass) {
        String[] segments = javaClass.getPackageName().split("\\.");
        return segments.length > 2 ? segments[2] : "";
    }
}
