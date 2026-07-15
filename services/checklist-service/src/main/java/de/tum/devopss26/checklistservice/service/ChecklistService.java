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

    GetChecklistsResponse getChecklists(Long userId);

    /**
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException if the checklist does not exist
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException if the user does not own it
     */
    GetChecklistResponse getChecklistById(Long userId, Long id);

    CreateChecklistResponse createChecklist(Long userId, Checklist checklist);

    UpdateChecklistResponse updateChecklist(Long userId, Long id, Checklist checklist);

    void deleteChecklist(Long userId, Long id);

    /**
     * If the item's position is not specified, it is assigned the next ordinal value ({@code items.size() + 1}).
     */
    AddChecklistItemResponse addChecklistItem(Long userId, Long checklistId, ChecklistItem item);

    /**
     * Validates that the item belongs to the specified checklist before applying changes.
     */
    UpdateChecklistItemResponse updateChecklistItem(Long userId, Long checklistId, Long itemId, ChecklistItem item);

    void deleteChecklistItem(Long userId, Long checklistId, Long itemId);
}
