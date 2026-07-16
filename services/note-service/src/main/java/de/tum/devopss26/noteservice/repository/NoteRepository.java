package de.tum.devopss26.noteservice.repository;

import de.tum.devopss26.noteservice.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for managing {@link Note} entities.
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Finds all notes belonging to the specified user.
     *
     * @param userId the ID of the user whose notes to find
     * @return a list of notes owned by the user, or an empty list if none exist
     */
    List<Note> findAllByUserId(long userId);

}
