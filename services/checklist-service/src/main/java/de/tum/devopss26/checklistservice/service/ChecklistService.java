package de.tum.devopss26.checklistservice.service;

import org.openapitools.model.AddChecklistItemResponse;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.openapitools.model.CreateChecklistResponse;
import org.openapitools.model.GetChecklistResponse;
import org.openapitools.model.GetChecklistsResponse;
import org.openapitools.model.UpdateChecklistItemResponse;
import org.openapitools.model.UpdateChecklistResponse;

/**
 * Every mutating method enforces ownership — a user may only access their own checklists —
 * and propagates the authenticated {@code userId} extracted from the request token rather
 * than accepting it from the client.
 */
public interface ChecklistService {

    /**
     * Retrieves all checklists belonging to the specified user.
     *
     * @param userId the ID of the user whose checklists to retrieve
     * @return a response containing the list of checklists
     */
    GetChecklistsResponse getChecklists(Long userId);

    /**
     * Retrieves a single checklist by its ID, verifying ownership.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the checklist to retrieve
     * @return a response containing the requested checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the user does not own the checklist
     */
    GetChecklistResponse getChecklistById(Long userId, Long id);

    /**
     * Creates a new checklist for the specified user.
     *
     * @param userId    the ID of the authenticated user
     * @param checklist the checklist data containing the title
     * @return a response containing the created checklist
     */
    CreateChecklistResponse createChecklist(Long userId, Checklist checklist);

    /**
     * Updates the title of an existing checklist. Ownership is enforced before applying changes.
     *
     * @param userId    the ID of the authenticated user
     * @param id        the ID of the checklist to update
     * @param checklist the checklist data containing the updated title
     * @return a response containing the updated checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the user does not own the checklist
     */
    UpdateChecklistResponse updateChecklist(Long userId, Long id, Checklist checklist);

    /**
     * Deletes a checklist. Ownership is enforced before deletion.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the checklist to delete
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the user does not own the checklist
     */
    void deleteChecklist(Long userId, Long id);

    /**
     * Adds a new item to a checklist. If the item's position is not specified, it is assigned
     * the next ordinal value ({@code items.size() + 1}).
     *
     * @param userId      the ID of the authenticated user
     * @param checklistId the ID of the checklist to add the item to
     * @param item        the item data containing text, completion status, and optional position
     * @return a response containing the created item
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the user does not own the checklist
     */
    AddChecklistItemResponse addChecklistItem(Long userId, Long checklistId, ChecklistItem item);

    /**
     * Updates an existing checklist item. Validates that the item belongs to the specified
     * checklist before applying changes.
     *
     * @param userId      the ID of the authenticated user
     * @param checklistId the ID of the checklist containing the item
     * @param itemId      the ID of the item to update
     * @param item        the item data containing updated fields
     * @return a response containing the updated item
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the user does not own the checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException
     *         if the item does not exist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException
     *         if the item does not belong to the specified checklist
     */
    UpdateChecklistItemResponse updateChecklistItem(Long userId, Long checklistId, Long itemId, ChecklistItem item);

    /**
     * Deletes a checklist item. Verifies that the item belongs to the specified checklist
     * before deletion.
     *
     * @param userId      the ID of the authenticated user
     * @param checklistId the ID of the checklist containing the item
     * @param itemId      the ID of the item to delete
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the user does not own the checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException
     *         if the item does not exist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException
     *         if the item does not belong to the specified checklist
     */
    void deleteChecklistItem(Long userId, Long checklistId, Long itemId);
}
