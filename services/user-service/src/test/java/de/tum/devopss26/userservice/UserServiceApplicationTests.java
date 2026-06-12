package de.tum.devopss26.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "USER_SERVICE_APP_PORT=8001",
    "USER_SERVICE_POSTGRES_URL=localhost",
    "USER_SERVICE_POSTGRES_PORT_INT=5441",
    "USER_SERVICE_POSTGRES_DB=public",
    "USER_SERVICE_POSTGRES_USER=admin",
    "USER_SERVICE_POSTGRES_PASSWORD=admin"
})
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
