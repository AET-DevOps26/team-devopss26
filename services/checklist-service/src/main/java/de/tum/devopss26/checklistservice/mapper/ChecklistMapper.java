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
 * The {@code toGetChecklistsResponse} convenience method wraps a list into the paginated
 * response envelope manually since the wrapper is a simple container.
 */
@Mapper(componentModel = "spring")
public interface ChecklistMapper {

    /**
     * Maps an {@link IdentifiedChecklistItem} to an {@link AddChecklistItemResponse}.
     *
     * @param item the item to map
     * @return the response DTO
     */
    AddChecklistItemResponse toAddChecklistItemResponse(IdentifiedChecklistItem item);

    /**
     * Maps an {@link IdentifiedChecklistItem} to an {@link UpdateChecklistItemResponse}.
     *
     * @param item the item to map
     * @return the response DTO
     */
    UpdateChecklistItemResponse toUpdateChecklistItemResponse(IdentifiedChecklistItem item);

    /**
     * Maps an {@link IdentifiedChecklist} to a {@link GetChecklistResponse}.
     *
     * @param checklist the checklist to map
     * @return the response DTO
     */
    GetChecklistResponse toGetChecklistResponse(IdentifiedChecklist checklist);

    /**
     * Maps an {@link IdentifiedChecklist} to a {@link CreateChecklistResponse}.
     *
     * @param checklist the checklist to map
     * @return the response DTO
     */
    CreateChecklistResponse toCreateChecklistResponse(IdentifiedChecklist checklist);

    /**
     * Maps an {@link IdentifiedChecklist} to an {@link UpdateChecklistResponse}.
     *
     * @param checklist the checklist to map
     * @return the response DTO
     */
    UpdateChecklistResponse toUpdateChecklistResponse(IdentifiedChecklist checklist);

    /**
     * Wraps a list of checklists into a {@link GetChecklistsResponse}.
     *
     * @param checklists the list of checklists to wrap
     * @return the response DTO containing the list
     */
    default GetChecklistsResponse toGetChecklistsResponse(List<IdentifiedChecklist> checklists) {
        return new GetChecklistsResponse().checklists(checklists);
    }

}
