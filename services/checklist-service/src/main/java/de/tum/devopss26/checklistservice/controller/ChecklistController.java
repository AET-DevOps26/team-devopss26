package de.tum.devopss26.checklistservice.controller;

import de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException;
import de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException;
import de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException;
import de.tum.devopss26.checklistservice.service.ChecklistService;
import de.tum.devopss26.shared.security.JWTHelper;
import de.tum.devopss26.shared.security.RequireTokenValidation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ChecklistsApi;
import org.openapitools.model.AddChecklistItemRequest;
import org.openapitools.model.AddChecklistItemResponse;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.openapitools.model.CreateChecklistRequest;
import org.openapitools.model.CreateChecklistResponse;
import org.openapitools.model.GetChecklistResponse;
import org.openapitools.model.GetChecklistsResponse;
import org.openapitools.model.IdentifiedChecklist;
import org.openapitools.model.IdentifiedChecklistItem;
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
    private final HttpServletRequest servletRequest;

    @RequireTokenValidation
    @Override
    public ResponseEntity<GetChecklistsResponse> getChecklists() {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        GetChecklistsResponse response = new GetChecklistsResponse()
                .checklists(checklistService.getChecklists(userId));
        return ResponseEntity.ok(response);
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<GetChecklistResponse> getChecklistById(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        IdentifiedChecklist checklist = checklistService.getChecklistById(userId, id);
        return ResponseEntity.ok(toGetChecklistResponse(checklist));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<CreateChecklistResponse> createChecklist(CreateChecklistRequest createChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toCreate = new Checklist().title(createChecklistRequest.getTitle());
        IdentifiedChecklist created = checklistService.createChecklist(userId, toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCreateChecklistResponse(created));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateChecklistResponse> updateChecklist(Long id, UpdateChecklistRequest updateChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toUpdate = new Checklist().title(updateChecklistRequest.getTitle());
        IdentifiedChecklist updated = checklistService.updateChecklist(userId, id, toUpdate);
        return ResponseEntity.ok(toUpdateChecklistResponse(updated));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteChecklist(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        checklistService.deleteChecklist(userId, id);
        return ResponseEntity.noContent().build();
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<AddChecklistItemResponse> addChecklistItem(Long id, AddChecklistItemRequest addChecklistItemRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        ChecklistItem toAdd = new ChecklistItem()
                .text(addChecklistItemRequest.getText())
                .completed(addChecklistItemRequest.getCompleted())
                .position(addChecklistItemRequest.getPosition());
        IdentifiedChecklistItem added = checklistService.addChecklistItem(userId, id, toAdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAddChecklistItemResponse(added));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateChecklistItemResponse> updateChecklistItem(Long id, Long itemId, ChecklistItem updateChecklistItemRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        ChecklistItem toUpdate = new ChecklistItem()
                .text(updateChecklistItemRequest.getText())
                .completed(updateChecklistItemRequest.getCompleted())
                .position(updateChecklistItemRequest.getPosition());
        IdentifiedChecklistItem updated = checklistService.updateChecklistItem(userId, id, itemId, toUpdate);
        return ResponseEntity.ok(toUpdateChecklistItemResponse(updated));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteChecklistItem(Long id, Long itemId) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        checklistService.deleteChecklistItem(userId, id, itemId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({ChecklistNotFoundException.class, ChecklistItemNotFoundException.class,
            ChecklistItemNotInChecklistException.class})
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    private AddChecklistItemResponse toAddChecklistItemResponse(IdentifiedChecklistItem item) {
        return new AddChecklistItemResponse()
                .id(item.getId())
                .text(item.getText())
                .completed(item.getCompleted())
                .position(item.getPosition());
    }

    private UpdateChecklistItemResponse toUpdateChecklistItemResponse(IdentifiedChecklistItem item) {
        return new UpdateChecklistItemResponse()
                .id(item.getId())
                .text(item.getText())
                .completed(item.getCompleted())
                .position(item.getPosition());
    }

    private GetChecklistResponse toGetChecklistResponse(IdentifiedChecklist checklist) {
        return new GetChecklistResponse()
                .id(checklist.getId())
                .userId(checklist.getUserId())
                .title(checklist.getTitle())
                .createdAt(checklist.getCreatedAt())
                .items(checklist.getItems());
    }

    private CreateChecklistResponse toCreateChecklistResponse(IdentifiedChecklist checklist) {
        return new CreateChecklistResponse()
                .id(checklist.getId())
                .userId(checklist.getUserId())
                .title(checklist.getTitle())
                .createdAt(checklist.getCreatedAt())
                .items(checklist.getItems());
    }

    private UpdateChecklistResponse toUpdateChecklistResponse(IdentifiedChecklist checklist) {
        return new UpdateChecklistResponse()
                .id(checklist.getId())
                .userId(checklist.getUserId())
                .title(checklist.getTitle())
                .createdAt(checklist.getCreatedAt())
                .items(checklist.getItems());
    }
}
