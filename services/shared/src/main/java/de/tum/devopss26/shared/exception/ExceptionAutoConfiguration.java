package de.tum.devopss26.shared.exception;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GlobalExceptionHandler.class)
public class ExceptionAutoConfiguration {
}
