package de.tum.devopss26.noteservice.repository;

import de.tum.devopss26.noteservice.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findAllByUserId(long userId);

}
