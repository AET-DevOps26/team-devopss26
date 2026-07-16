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
 * Service interface for checklist management operations.
 * Defines the business logic contract for creating, reading, updating,
 * and deleting checklists and their items, with ownership validation.
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
     * Retrieves a specific checklist by ID, validating that it belongs to the given user.
     *
     * @param userId the ID of the user requesting the checklist
     * @param id     the ID of the checklist to retrieve
     * @return a response containing the requested checklist details
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException if the user does not own the checklist
     */
    GetChecklistResponse getChecklistById(Long userId, Long id);

    /**
     * Creates a new checklist for the specified user.
     *
     * @param userId   the ID of the user who will own the checklist
     * @param checklist the checklist data to create
     * @return a response containing the created checklist details
     */
    CreateChecklistResponse createChecklist(Long userId, Checklist checklist);

    /**
     * Updates an existing checklist, validating ownership.
     *
     * @param userId   the ID of the user requesting the update
     * @param id       the ID of the checklist to update
     * @param checklist the updated checklist data
     * @return a response containing the updated checklist details
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException if the user does not own the checklist
     */
    UpdateChecklistResponse updateChecklist(Long userId, Long id, Checklist checklist);

    /**
     * Deletes a checklist and all its items, validating ownership.
     *
     * @param userId the ID of the user requesting the deletion
     * @param id     the ID of the checklist to delete
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException if the user does not own the checklist
     */
    void deleteChecklist(Long userId, Long id);

    /**
     * Adds a new item to an existing checklist, validating ownership.
     *
     * @param userId      the ID of the user who owns the checklist
     * @param checklistId the ID of the checklist to add the item to
     * @param item        the item data to add
     * @return a response containing the created item details
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException if the user does not own the checklist
     */
    AddChecklistItemResponse addChecklistItem(Long userId, Long checklistId, ChecklistItem item);

    /**
     * Updates an existing checklist item, validating checklist ownership
     * and that the item belongs to the specified checklist.
     *
     * @param userId      the ID of the user who owns the checklist
     * @param checklistId the ID of the checklist containing the item
     * @param itemId      the ID of the item to update
     * @param item        the updated item data
     * @return a response containing the updated item details
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException if the user does not own the checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException if the item does not exist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException if the item does not belong to the checklist
     */
    UpdateChecklistItemResponse updateChecklistItem(Long userId, Long checklistId, Long itemId, ChecklistItem item);

    /**
     * Deletes a checklist item, validating checklist ownership
     * and that the item belongs to the specified checklist.
     *
     * @param userId      the ID of the user who owns the checklist
     * @param checklistId the ID of the checklist containing the item
     * @param itemId      the ID of the item to delete
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException if the user does not own the checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException if the item does not exist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException if the item does not belong to the checklist
     */
    void deleteChecklistItem(Long userId, Long checklistId, Long itemId);
}
