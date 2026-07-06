package de.tum.devopss26.checklistservice.controller;

import de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException;
import de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException;
import de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException;
import de.tum.devopss26.checklistservice.service.ChecklistService;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ChecklistsApi;
import org.openapitools.model.AddChecklistItemRequest;
import org.openapitools.model.AddChecklistItemResponse;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.openapitools.model.CreateChecklistRequest;
import org.openapitools.model.CreateChecklistResponse;
import org.openapitools.model.GetChecklistByIdResponse;
import org.openapitools.model.GetChecklistsResponse;
import org.openapitools.model.UpdateChecklistItemRequest;
import org.openapitools.model.UpdateChecklistItemResponse;
import org.openapitools.model.UpdateChecklistRequest;
import org.openapitools.model.UpdateChecklistResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChecklistController implements ChecklistsApi {

    private final ChecklistService checklistService;

    @Override
    public ResponseEntity<GetChecklistsResponse> getChecklists(Long userId) {
        GetChecklistsResponse response = new GetChecklistsResponse()
                .checklists(checklistService.getChecklists(userId));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<GetChecklistByIdResponse> getChecklistById(Long id) {
        Checklist checklist = checklistService.getChecklistById(id);
        return ResponseEntity.ok(toGetChecklistByIdResponse(checklist));
    }

    @Override
    public ResponseEntity<CreateChecklistResponse> createChecklist(CreateChecklistRequest createChecklistRequest) {
        Checklist toCreate = new Checklist().title(createChecklistRequest.getTitle());
        Checklist created = checklistService.createChecklist(createChecklistRequest.getUserId(), toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCreateChecklistResponse(created));
    }

    @Override
    public ResponseEntity<UpdateChecklistResponse> updateChecklist(Long id, UpdateChecklistRequest updateChecklistRequest) {
        Checklist toUpdate = new Checklist().title(updateChecklistRequest.getTitle());
        Checklist updated = checklistService.updateChecklist(id, toUpdate);
        return ResponseEntity.ok(toUpdateChecklistResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteChecklist(Long id) {
        checklistService.deleteChecklist(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AddChecklistItemResponse> addChecklistItem(Long id, AddChecklistItemRequest addChecklistItemRequest) {
        ChecklistItem toAdd = new ChecklistItem()
                .text(addChecklistItemRequest.getText())
                .completed(addChecklistItemRequest.getCompleted())
                .position(addChecklistItemRequest.getPosition());
        ChecklistItem added = checklistService.addChecklistItem(id, toAdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAddChecklistItemResponse(added));
    }

    @Override
    public ResponseEntity<UpdateChecklistItemResponse> updateChecklistItem(Long id, Long itemId, UpdateChecklistItemRequest updateChecklistItemRequest) {
        ChecklistItem toUpdate = new ChecklistItem()
                .text(updateChecklistItemRequest.getText())
                .completed(updateChecklistItemRequest.getCompleted())
                .position(updateChecklistItemRequest.getPosition());
        ChecklistItem updated = checklistService.updateChecklistItem(id, itemId, toUpdate);
        return ResponseEntity.ok(toUpdateChecklistItemResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteChecklistItem(Long id, Long itemId) {
        checklistService.deleteChecklistItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({ChecklistNotFoundException.class, ChecklistItemNotFoundException.class,
            ChecklistItemNotInChecklistException.class})
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    private GetChecklistByIdResponse toGetChecklistByIdResponse(Checklist checklist) {
        return new GetChecklistByIdResponse()
                .id(checklist.getId())
                .userId(checklist.getUserId())
                .title(checklist.getTitle())
                .createdAt(checklist.getCreatedAt())
                .items(checklist.getItems());
    }

    private CreateChecklistResponse toCreateChecklistResponse(Checklist checklist) {
        return new CreateChecklistResponse()
                .id(checklist.getId())
                .userId(checklist.getUserId())
                .title(checklist.getTitle())
                .createdAt(checklist.getCreatedAt())
                .items(checklist.getItems());
    }

    private UpdateChecklistResponse toUpdateChecklistResponse(Checklist checklist) {
        return new UpdateChecklistResponse()
                .id(checklist.getId())
                .userId(checklist.getUserId())
                .title(checklist.getTitle())
                .createdAt(checklist.getCreatedAt())
                .items(checklist.getItems());
    }

    private AddChecklistItemResponse toAddChecklistItemResponse(ChecklistItem item) {
        return new AddChecklistItemResponse()
                .id(item.getId())
                .text(item.getText())
                .completed(item.getCompleted())
                .position(item.getPosition());
    }

    private UpdateChecklistItemResponse toUpdateChecklistItemResponse(ChecklistItem item) {
        return new UpdateChecklistItemResponse()
                .id(item.getId())
                .text(item.getText())
                .completed(item.getCompleted())
                .position(item.getPosition());
    }
}
