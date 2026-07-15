package de.tum.devopss26.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <b>Constraints:</b>
 * <ul>
 *   <li>{@code username} — must be unique ({@code UNIQUE} constraint) and never null.
 *       The uniqueness is enforced at the database level to prevent race-condition duplicates
 *       that application-level checks alone cannot prevent.</li>
 *   <li>{@code passwordHash} — never null. Stores a BCrypt hash (strength 12), never a
 *       plain-text password. The encoding is performed by {@link de.tum.devopss26.userservice.mapper.UserMapper}
 *       during registration.</li>
 * </ul>
 *
 * @see de.tum.devopss26.userservice.repository.UserRepository
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
