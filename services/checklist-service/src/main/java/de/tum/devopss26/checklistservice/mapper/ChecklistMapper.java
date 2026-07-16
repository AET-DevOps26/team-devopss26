package de.tum.devopss26.checklistservice.mapper;

import org.mapstruct.Mapper;
import org.openapitools.model.AddChecklistItemResponse;
import org.openapitools.model.CreateChecklistResponse;
import org.openapitools.model.GetChecklistResponse;
import org.openapitools.model.GetChecklistsResponse;
import org.openapitools.model.IdentifiedChecklist;
import org.openapitools.model.IdentifiedChecklistItem;
import org.openapitools.model.UpdateChecklistItemResponse;
import org.openapitools.model.UpdateChecklistResponse;

import java.util.List;

/**
 * MapStruct mapper for converting between domain DTOs and API response models.
 * Generates Spring-managed mapper implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface ChecklistMapper {

    /**
     * Maps an identified checklist item to an {@link AddChecklistItemResponse}.
     *
     * @param item the identified checklist item to map
     * @return the add-checklist-item response
     */
    AddChecklistItemResponse toAddChecklistItemResponse(IdentifiedChecklistItem item);

    /**
     * Maps an identified checklist item to an {@link UpdateChecklistItemResponse}.
     *
     * @param item the identified checklist item to map
     * @return the update-checklist-item response
     */
    UpdateChecklistItemResponse toUpdateChecklistItemResponse(IdentifiedChecklistItem item);

    /**
     * Maps an identified checklist to a {@link GetChecklistResponse}.
     *
     * @param checklist the identified checklist to map
     * @return the get-checklist response
     */
    GetChecklistResponse toGetChecklistResponse(IdentifiedChecklist checklist);

    /**
     * Maps an identified checklist to a {@link CreateChecklistResponse}.
     *
     * @param checklist the identified checklist to map
     * @return the create-checklist response
     */
    CreateChecklistResponse toCreateChecklistResponse(IdentifiedChecklist checklist);

    /**
     * Maps an identified checklist to an {@link UpdateChecklistResponse}.
     *
     * @param checklist the identified checklist to map
     * @return the update-checklist response
     */
    UpdateChecklistResponse toUpdateChecklistResponse(IdentifiedChecklist checklist);

    /**
     * Wraps a list of identified checklists into a {@link GetChecklistsResponse}.
     *
     * @param checklists the list of identified checklists
     * @return the get-checklists response containing the list
     */
    default GetChecklistsResponse toGetChecklistsResponse(List<IdentifiedChecklist> checklists) {
        return new GetChecklistsResponse().checklists(checklists);
    }

}
