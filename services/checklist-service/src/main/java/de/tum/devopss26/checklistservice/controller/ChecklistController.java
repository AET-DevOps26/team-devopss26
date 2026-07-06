package de.tum.devopss26.checklistservice.controller;

import de.tum.devopss26.checklistservice.service.ChecklistService;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ChecklistsApi;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChecklistController implements ChecklistsApi {

    private final ChecklistService checklistService;

    @Override
    public ResponseEntity<List<Checklist>> getChecklists(Long userId) {
        return ResponseEntity.ok(checklistService.getChecklists(userId));
    }

    @Override
    public ResponseEntity<Checklist> getChecklistById(Long id) {
        return ResponseEntity.ok(checklistService.getChecklistById(id));
    }

    @Override
    public ResponseEntity<Checklist> createChecklist(Checklist checklist) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checklistService.createChecklist(checklist.getUserId(), checklist));
    }

    @Override
    public ResponseEntity<Checklist> updateChecklist(Long id, Checklist checklist) {
        return ResponseEntity.ok(checklistService.updateChecklist(id, checklist));
    }

    @Override
    public ResponseEntity<Void> deleteChecklist(Long id) {
        checklistService.deleteChecklist(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ChecklistItem> addChecklistItem(Long id, ChecklistItem checklistItem) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checklistService.addChecklistItem(id, checklistItem));
    }

    @Override
    public ResponseEntity<ChecklistItem> updateChecklistItem(Long id, Long itemId, ChecklistItem checklistItem) {
        return ResponseEntity.ok(checklistService.updateChecklistItem(id, itemId, checklistItem));
    }

    @Override
    public ResponseEntity<Void> deleteChecklistItem(Long id, Long itemId) {
        checklistService.deleteChecklistItem(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
