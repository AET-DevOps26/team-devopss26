package de.tum.devopss26.checklistservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checklists")
@Tag(name = "Checklists", description = "Operations for managing checklists and their items")
public class ChecklistController {

    @GetMapping
    @Operation(summary = "Get all checklists for a user")
    public ResponseEntity<List<Checklist>> getAllChecklists(@RequestParam Long userId) {
        return ResponseEntity.ok(List.of(new Checklist(1L, "Sample Checklist")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a checklist by ID", description = "Returns the checklist along with all its items")
    public ResponseEntity<Checklist> getChecklistById(@PathVariable Long id) {
        return ResponseEntity.ok(new Checklist(id, "Sample Checklist"));
    }

    @PostMapping
    @Operation(summary = "Create a new checklist")
    public ResponseEntity<Checklist> createChecklist(@RequestBody Checklist checklist) {
        checklist.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(checklist);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a checklist's title")
    public ResponseEntity<Checklist> updateChecklist(@PathVariable Long id, @RequestBody Checklist checklist) {
        checklist.setId(id);
        return ResponseEntity.ok(checklist);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a checklist by ID")
    public ResponseEntity<Void> deleteChecklist(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Add an item to a checklist")
    public ResponseEntity<ChecklistItem> addItem(@PathVariable Long id, @RequestBody ChecklistItem item) {
        item.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "Update a checklist item", description = "Can be used to change the text, toggle completed, or reorder")
    public ResponseEntity<ChecklistItem> updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody ChecklistItem item) {
        item.setId(itemId);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Remove an item from a checklist")
    public ResponseEntity<Void> removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return ResponseEntity.noContent().build();
    }
}
