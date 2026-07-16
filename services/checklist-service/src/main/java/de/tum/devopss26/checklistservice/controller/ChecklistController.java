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
 * Every endpoint is guarded by {@link RequireTokenValidation}. The authenticated user's ID is
 * extracted from the token via {@link JWTHelper} and passed to the service layer — the client
 * never supplies the userId directly (preventing privilege escalation).
 * <p>Checklist-item sub-resources follow the pattern {@code /checklists/{id}/items/{itemId}}.</p>
 */
@RestController
@RequiredArgsConstructor
public class ChecklistController implements ChecklistsApi {

    private final ChecklistService checklistService;
    private final HttpServletRequest servletRequest;

    /**
     * Retrieves all checklists for the authenticated user.
     *
     * @return {@code 200 OK} with a list of the user's checklists
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<GetChecklistsResponse> getChecklists() {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        return ResponseEntity.ok(checklistService.getChecklists(userId));
    }

    /**
     * Retrieves a checklist by its ID. The checklist must belong to the authenticated user.
     *
     * @param id the ID of the checklist to retrieve
     * @return a response containing the requested checklist
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
     * @param createChecklistRequest the request containing the checklist title
     * @return a response containing the created checklist
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<CreateChecklistResponse> createChecklist(CreateChecklistRequest createChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toCreate = new Checklist().title(createChecklistRequest.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.createChecklist(userId, toCreate));
    }

    /**
     * Updates a checklist's title. Ownership is enforced server-side.
     *
     * @param id                      the ID of the checklist to update
     * @param updateChecklistRequest  the request containing the updated title
     * @return a response containing the updated checklist
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateChecklistResponse> updateChecklist(Long id, UpdateChecklistRequest updateChecklistRequest) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        Checklist toUpdate = new Checklist().title(updateChecklistRequest.getTitle());
        return ResponseEntity.ok(checklistService.updateChecklist(userId, id, toUpdate));
    }

    /**
     * Deletes a checklist. Ownership is enforced server-side.
     *
     * @param id the ID of the checklist to delete
     * @return a response with no content
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteChecklist(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        checklistService.deleteChecklist(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a new item to a checklist. The position defaults to the end of the list if not specified.
     *
     * @param id                      the ID of the checklist to add the item to
     * @param addChecklistItemRequest the request containing the item details
     * @return a response containing the created item
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
     * Updates a checklist item. Validates that the item belongs to the specified checklist.
     *
     * @param id                          the ID of the checklist containing the item
     * @param itemId                      the ID of the item to update
     * @param updateChecklistItemRequest  the request containing the updated item details
     * @return a response containing the updated item
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
     * Deletes a checklist item.
     *
     * @param id     the ID of the checklist containing the item
     * @param itemId the ID of the item to delete
     * @return a response with no content
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteChecklistItem(Long id, Long itemId) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        checklistService.deleteChecklistItem(userId, id, itemId);
        return ResponseEntity.noContent().build();
    }

}
