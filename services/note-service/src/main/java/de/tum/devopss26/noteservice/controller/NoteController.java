package de.tum.devopss26.noteservice.controller;

import org.openapitools.api.NotesApi;
import org.openapitools.model.Note;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NoteController implements NotesApi {

    @Override
    public ResponseEntity<List<Note>> getNotes(Long userId) {
        Note note1 = new Note();
        note1.setId(1L);
        note1.setTitle("Car Service");
        note1.setContent("Need to take the car in for an oil change. Also ask them to check the tire pressure and brake pads.");

        Note note2 = new Note();
        note2.setId(2L);
        note2.setTitle("Post Office");
        note2.setContent("Send the birthday package to aunt Maria. Remember to use express shipping so it arrives before Saturday.");

        Note note3 = new Note();
        note3.setId(3L);
        note3.setTitle("Pharmacy");
        note3.setContent("Pick up prescription for blood pressure medication. Also grab some vitamin D supplements and a thermometer.");

        return ResponseEntity.ok(List.of(note1, note2, note3));
    }

    @Override
    public ResponseEntity<Note> getNoteById(Long id) {
        Note note = new Note();
        note.setId(id);
        note.setTitle("Sample Note");
        note.setContent("Sample content");
        return ResponseEntity.ok(note);
    }

    @Override
    public ResponseEntity<Note> createNote(Note note) {
        note.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @Override
    public ResponseEntity<Note> updateNote(Long id, Note note) {
        note.setId(id);
        return ResponseEntity.ok(note);
    }

    @Override
    public ResponseEntity<Void> deleteNote(Long id) {
        return ResponseEntity.noContent().build();
    }
}
