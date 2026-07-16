package de.tum.devopss26.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a registered user in the system.
 * Each user has a unique username and a BCrypt-hashed password.
 */
@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;
}
