package de.tum.devopss26.checklistservice.controller;

import de.tum.devopss26.checklistservice.service.ChecklistService;
import de.tum.devopss26.shared.security.JWTHelper;
import de.tum.devopss26.shared.security.RequireTokenValidation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ChecklistsApi;
import org.openapitools.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        return ResponseEntity.ok(checklistService.getChecklists(userId));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<GetChecklistResponse> getChecklistById(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        return ResponseEntity.ok(checklistService.getChecklistById(userId, id));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<CreateChecklistResponse> createChecklist(CreateChecklistRequest createChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toCreate = new Checklist().title(createChecklistRequest.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.createChecklist(userId, toCreate));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateChecklistResponse> updateChecklist(Long id, UpdateChecklistRequest updateChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toUpdate = new Checklist().title(updateChecklistRequest.getTitle());
        return ResponseEntity.ok(checklistService.updateChecklist(userId, id, toUpdate));
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
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.addChecklistItem(userId, id, toAdd));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateChecklistItemResponse> updateChecklistItem(Long id, Long itemId, ChecklistItem updateChecklistItemRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        ChecklistItem toUpdate = new ChecklistItem()
                .text(updateChecklistItemRequest.getText())
                .completed(updateChecklistItemRequest.getCompleted())
                .position(updateChecklistItemRequest.getPosition());
        return ResponseEntity.ok(checklistService.updateChecklistItem(userId, id, itemId, toUpdate));
    }

    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteChecklistItem(Long id, Long itemId) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        checklistService.deleteChecklistItem(userId, id, itemId);
        return ResponseEntity.noContent().build();
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
