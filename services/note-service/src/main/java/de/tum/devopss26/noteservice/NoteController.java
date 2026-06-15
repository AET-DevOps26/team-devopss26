package de.tum.devopss26.noteservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@Tag(name = "Notes", description = "Operations for managing notes")
public class NoteController {

    @GetMapping
    @Operation(summary = "Get all notes for a user", description = "Returns all notes belonging to the specified user")
    public ResponseEntity<List<Note>> getAllNotes(@RequestParam Long userId) {
        return ResponseEntity.ok(List.of(
                new Note(1L, "Sample Note", "Sample content")
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a note by ID")
    public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(new Note(id, "Sample Note", "Sample content"));
    }

    @PostMapping
    @Operation(summary = "Create a new note")
    public ResponseEntity<Note> createNote(@RequestBody Note note) {
        note.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a note's title and content")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody Note note) {
        note.setId(id);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a note by ID")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
