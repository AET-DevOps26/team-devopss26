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
                new Note(1L, "Car Service", "Need to take the car in for an oil change. Also ask them to check the tire pressure and brake pads."),
                new Note(2L, "Post Office", "Send the birthday package to aunt Maria. Remember to use express shipping so it arrives before Saturday."),
                new Note(3L, "Pharmacy", "Pick up prescription for blood pressure medication. Also grab some vitamin D supplements and a thermometer.")
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
