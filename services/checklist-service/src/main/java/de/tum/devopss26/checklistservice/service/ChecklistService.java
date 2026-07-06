package de.tum.devopss26.checklistservice.service;

import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;

import java.util.List;

public interface ChecklistService {

    List<Checklist> getChecklists(Long userId);

    Checklist getChecklistById(Long id);

    Checklist createChecklist(Long userId, Checklist checklist);

    Checklist updateChecklist(Long id, Checklist checklist);

    void deleteChecklist(Long id);

    ChecklistItem addChecklistItem(Long checklistId, ChecklistItem item);

    ChecklistItem updateChecklistItem(Long checklistId, Long itemId, ChecklistItem item);

    void deleteChecklistItem(Long checklistId, Long itemId);
}
