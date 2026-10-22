package com.uc.ms_security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "security.jwt.secret=test-only-signing-key-at-least-32-bytes")
class MsSecurityApplicationTests {

	@Test
	void contextLoads() {
	}

}
