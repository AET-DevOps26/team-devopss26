package de.tum.devopss26.calendarservice;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import de.tum.devopss26.shared.arch.AbstractArchitectureTest;

public class ArchitectureTest extends AbstractArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("de.tum.devopss26.calendarservice");

    @Override
    protected JavaClasses getClasses() {
        return classes;
    }
}
