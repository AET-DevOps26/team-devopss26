package de.tum.devopss26.shared.it;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for integration tests that require a running PostgreSQL
 * instance provided by Testcontainers.
 * <p>
 * Subclasses automatically get a {@link ServiceConnection} to the container,
 * so no manual DataSource configuration is needed.
 * </p>
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

	/**
	 * Shared PostgreSQL container started once for all tests in the hierarchy.
	 */
	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12.8");

}
