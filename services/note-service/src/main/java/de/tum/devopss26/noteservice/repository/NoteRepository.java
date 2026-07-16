package de.tum.devopss26.noteservice.repository;

import de.tum.devopss26.noteservice.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Note} entities.
 * <p>
 * Provides database access for note CRUD operations including custom queries
 * to retrieve notes by their owning user.
 * </p>
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Retrieves all notes belonging to the specified user.
     *
     * @param userId the ID of the user whose notes to retrieve
     * @return a list of notes owned by the user, or an empty list if none exist
     */
    List<Note> findAllByUserId(long userId);

}
