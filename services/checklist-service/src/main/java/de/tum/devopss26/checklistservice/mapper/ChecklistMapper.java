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

    AddChecklistItemResponse toAddChecklistItemResponse(IdentifiedChecklistItem item);

    UpdateChecklistItemResponse toUpdateChecklistItemResponse(IdentifiedChecklistItem item);

    GetChecklistResponse toGetChecklistResponse(IdentifiedChecklist checklist);

    CreateChecklistResponse toCreateChecklistResponse(IdentifiedChecklist checklist);

    UpdateChecklistResponse toUpdateChecklistResponse(IdentifiedChecklist checklist);

    default GetChecklistsResponse toGetChecklistsResponse(List<IdentifiedChecklist> checklists) {
        return new GetChecklistsResponse().checklists(checklists);
    }

}
