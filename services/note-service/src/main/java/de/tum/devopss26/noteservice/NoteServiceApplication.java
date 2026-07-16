package de.tum.devopss26.noteservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Note Service microservice.
 * <p>
 * Bootstraps the Spring Boot application context and starts the embedded web server.
 * This service provides RESTful API endpoints for managing notes with user-based
 * access control.
 * </p>
 */
@SpringBootApplication
public class NoteServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoteServiceApplication.class, args);
	}

}