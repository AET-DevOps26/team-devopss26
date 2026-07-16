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

/**
 * REST controller exposing endpoints for checklist and checklist item management.
 * All endpoints require token validation via {@link RequireTokenValidation} and
 * extract the authenticated user ID from the JWT in the request header.
 */
@RestController
@RequiredArgsConstructor
public class ChecklistController implements ChecklistsApi {

    private final ChecklistService checklistService;
    private final HttpServletRequest servletRequest;

    /**
     * Retrieves all checklists belonging to the authenticated user.
     *
     * @return a response containing the list of checklists with HTTP 200
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<GetChecklistsResponse> getChecklists() {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        return ResponseEntity.ok(checklistService.getChecklists(userId));
    }

    /**
     * Retrieves a single checklist by its ID, validating that it belongs
     * to the authenticated user.
     *
     * @param id the ID of the checklist to retrieve
     * @return a response containing the checklist details with HTTP 200
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<GetChecklistResponse> getChecklistById(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        return ResponseEntity.ok(checklistService.getChecklistById(userId, id));
    }

    /**
     * Creates a new checklist for the authenticated user.
     *
     * @param createChecklistRequest the request body containing the checklist title
     * @return a response containing the created checklist with HTTP 201
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<CreateChecklistResponse> createChecklist(CreateChecklistRequest createChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toCreate = new Checklist().title(createChecklistRequest.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.createChecklist(userId, toCreate));
    }

    /**
     * Updates an existing checklist, validating that it belongs to the authenticated user.
     *
     * @param id                      the ID of the checklist to update
     * @param updateChecklistRequest  the request body containing the updated title
     * @return a response containing the updated checklist with HTTP 200
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateChecklistResponse> updateChecklist(Long id, UpdateChecklistRequest updateChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toUpdate = new Checklist().title(updateChecklistRequest.getTitle());
        return ResponseEntity.ok(checklistService.updateChecklist(userId, id, toUpdate));
    }

    /**
     * Deletes a checklist and all its items, validating ownership.
     *
     * @param id the ID of the checklist to delete
     * @return HTTP 204 No Content on successful deletion
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteChecklist(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        checklistService.deleteChecklist(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a new item to an existing checklist.
     *
     * @param id                      the ID of the checklist to add the item to
     * @param addChecklistItemRequest the request body containing the item details
     * @return a response containing the created item with HTTP 201
     */
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

    /**
     * Updates an existing checklist item, validating checklist ownership
     * and that the item belongs to the specified checklist.
     *
     * @param id                          the ID of the checklist
     * @param itemId                      the ID of the item to update
     * @param updateChecklistItemRequest  the request body containing the updated item details
     * @return a response containing the updated item with HTTP 200
     */
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

    /**
     * Deletes a checklist item, validating checklist ownership
     * and that the item belongs to the specified checklist.
     *
     * @param id      the ID of the checklist
     * @param itemId  the ID of the item to delete
     * @return HTTP 204 No Content on successful deletion
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteChecklistItem(Long id, Long itemId) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        checklistService.deleteChecklistItem(userId, id, itemId);
        return ResponseEntity.noContent().build();
    }

}
