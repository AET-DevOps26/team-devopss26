package de.tum.devopss26.checklistservice.service;

import org.openapitools.model.AddChecklistItemResponse;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.openapitools.model.CreateChecklistResponse;
import org.openapitools.model.GetChecklistResponse;
import org.openapitools.model.GetChecklistsResponse;
import org.openapitools.model.UpdateChecklistItemResponse;
import org.openapitools.model.UpdateChecklistResponse;

public interface ChecklistService {

    GetChecklistsResponse getChecklists(Long userId);

    GetChecklistResponse getChecklistById(Long userId, Long id);

    CreateChecklistResponse createChecklist(Long userId, Checklist checklist);

    UpdateChecklistResponse updateChecklist(Long userId, Long id, Checklist checklist);

    void deleteChecklist(Long userId, Long id);

    AddChecklistItemResponse addChecklistItem(Long userId, Long checklistId, ChecklistItem item);

    UpdateChecklistItemResponse updateChecklistItem(Long userId, Long checklistId, Long itemId, ChecklistItem item);

    void deleteChecklistItem(Long userId, Long checklistId, Long itemId);
}
