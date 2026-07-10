package de.tum.devopss26.checklistservice.service;

import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.openapitools.model.IdentifiedChecklist;
import org.openapitools.model.IdentifiedChecklistItem;

import java.util.List;

public interface ChecklistService {

    List<IdentifiedChecklist> getChecklists(Long userId);

    IdentifiedChecklist getChecklistById(Long userId, Long id);

    IdentifiedChecklist createChecklist(Long userId, Checklist checklist);

    IdentifiedChecklist updateChecklist(Long userId, Long id, Checklist checklist);

    void deleteChecklist(Long userId, Long id);

    IdentifiedChecklistItem addChecklistItem(Long userId, Long checklistId, ChecklistItem item);

    IdentifiedChecklistItem updateChecklistItem(Long userId, Long checklistId, Long itemId, ChecklistItem item);

    void deleteChecklistItem(Long userId, Long checklistId, Long itemId);
}
