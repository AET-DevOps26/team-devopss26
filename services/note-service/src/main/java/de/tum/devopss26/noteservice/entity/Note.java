package de.tum.devopss26.noteservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Each note has a mandatory title (max 255 chars) and content stored as TEXT.
 * The owning user is identified by {@code userId}. Timestamps ({@code createdAt},
 * {@code lastUpdatedAt}) are set on creation and updated on mutation.
 */
@Getter
@Setter
@Entity
@Table(name = "note")
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private OffsetDateTime lastUpdatedAt;

}
