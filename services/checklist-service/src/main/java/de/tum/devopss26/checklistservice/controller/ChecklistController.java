package de.tum.devopss26.checklistservice.controller;

import org.openapitools.api.ChecklistsApi;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChecklistController implements ChecklistsApi {

    @Override
    public ResponseEntity<List<Checklist>> getChecklists(Long userId) {
        Checklist groceries = new Checklist();
        groceries.setId(1L);
        groceries.setTitle("Grocery Shopping");

        ChecklistItem item1 = new ChecklistItem();
        item1.setId(1L);
        item1.setText("Milk");
        item1.setCompleted(true);
        item1.setPosition(1);

        ChecklistItem item2 = new ChecklistItem();
        item2.setId(2L);
        item2.setText("Eggs");
        item2.setCompleted(false);
        item2.setPosition(2);

        ChecklistItem item3 = new ChecklistItem();
        item3.setId(3L);
        item3.setText("Bread");
        item3.setCompleted(false);
        item3.setPosition(3);

        ChecklistItem item4 = new ChecklistItem();
        item4.setId(4L);
        item4.setText("Coffee");
        item4.setCompleted(false);
        item4.setPosition(4);

        ChecklistItem item5 = new ChecklistItem();
        item5.setId(5L);
        item5.setText("Orange juice");
        item5.setCompleted(false);
        item5.setPosition(5);

        groceries.setItems(List.of(item1, item2, item3, item4, item5));

        Checklist errands = new Checklist();
        errands.setId(2L);
        errands.setTitle("Weekly Errands");

        ChecklistItem item6 = new ChecklistItem();
        item6.setId(6L);
        item6.setText("Drop off dry cleaning");
        item6.setCompleted(true);
        item6.setPosition(1);

        ChecklistItem item7 = new ChecklistItem();
        item7.setId(7L);
        item7.setText("Renew car insurance");
        item7.setCompleted(false);
        item7.setPosition(2);

        ChecklistItem item8 = new ChecklistItem();
        item8.setId(8L);
        item8.setText("Pay electricity bill");
        item8.setCompleted(false);
        item8.setPosition(3);

        ChecklistItem item9 = new ChecklistItem();
        item9.setId(9L);
        item9.setText("Return library books");
        item9.setCompleted(false);
        item9.setPosition(4);

        errands.setItems(List.of(item6, item7, item8, item9));

        return ResponseEntity.ok(List.of(groceries, errands));
    }

    @Override
    public ResponseEntity<Checklist> getChecklistById(Long id) {
        Checklist checklist = new Checklist();
        checklist.setId(id);
        checklist.setTitle("Sample Checklist");
        return ResponseEntity.ok(checklist);
    }

    @Override
    public ResponseEntity<Checklist> createChecklist(Checklist checklist) {
        checklist.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(checklist);
    }

    @Override
    public ResponseEntity<Checklist> updateChecklist(Long id, Checklist checklist) {
        checklist.setId(id);
        return ResponseEntity.ok(checklist);
    }

    @Override
    public ResponseEntity<Void> deleteChecklist(Long id) {
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ChecklistItem> addChecklistItem(Long id, ChecklistItem checklistItem) {
        checklistItem.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistItem);
    }

    @Override
    public ResponseEntity<ChecklistItem> updateChecklistItem(Long id, Long itemId, ChecklistItem checklistItem) {
        checklistItem.setId(itemId);
        return ResponseEntity.ok(checklistItem);
    }

    @Override
    public ResponseEntity<Void> deleteChecklistItem(Long id, Long itemId) {
        return ResponseEntity.noContent().build();
    }
}
