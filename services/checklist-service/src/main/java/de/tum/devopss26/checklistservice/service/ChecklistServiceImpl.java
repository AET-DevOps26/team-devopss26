package de.tum.devopss26.checklistservice.service;

import de.tum.devopss26.checklistservice.entity.Checklist;
import de.tum.devopss26.checklistservice.entity.ChecklistItem;
import de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException;
import de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException;
import de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException;
import de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException;
import de.tum.devopss26.checklistservice.mapper.ChecklistMapper;
import de.tum.devopss26.checklistservice.repository.ChecklistItemRepository;
import de.tum.devopss26.checklistservice.repository.ChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * All mutating operations are wrapped in a single transaction ({@link Transactional}) to ensure
 * consistency between checklist and item state.
 * <p><b>Ownership model:</b> every checklist is scoped to a {@code userId}. The private helper
 * {@link #getOwnedChecklistEntity} loads the entity and throws
 * {@link de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException} if the
 * caller does not own it. This guard is the single point of ownership enforcement for all
 * operations.
 * <p><b>Item lifecycle:</b> items use a manual persistence pattern rather than depending on
 * the parent's cascade — each item is explicitly saved/deleted via its own repository. This
 * avoids loading the entire item collection just to add or remove a single entry. The parent
 * checklist's {@code CascadeType.ALL} / {@code orphanRemoval} is configured but only exercised
 * when the parent itself is deleted.
 * <p><b>Diff-based updates:</b> item updates are field-by-field replacements (no structural diff).
 * The position is only updated when the request explicitly provides a non-null value; otherwise
 * the existing order is preserved. Ownership of an item is verified by cross-checking its
 * {@code checklist_id} against the parent checklist ID.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChecklistServiceImpl implements ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChecklistMapper mapper;

    /**
     * Retrieves all checklists belonging to the specified user.
     *
     * @param userId the ID of the user whose checklists to retrieve
     * @return a response containing the list of checklists
     */
    @Override
    public GetChecklistsResponse getChecklists(Long userId) {
        return mapper.toGetChecklistsResponse(checklistRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList());
    }

    /**
     * Retrieves a single checklist by its ID, verifying that the user owns it.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the checklist to retrieve
     * @return a response containing the requested checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if no checklist with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the checklist does not belong to the specified user
     */
    @Override
    public GetChecklistResponse getChecklistById(Long userId, Long id) {
        Checklist entity = getOwnedChecklistEntity(userId, id);
        return mapper.toGetChecklistResponse(toDto(entity));
    }

    /**
     * Creates a new checklist for the specified user. Assigns the current timestamp as
     * {@code createdAt} and starts with an empty items list.
     *
     * @param userId    the ID of the authenticated user
     * @param checklist the checklist data containing the title
     * @return a response containing the created checklist
     */
    @Override
    public CreateChecklistResponse createChecklist(Long userId, org.openapitools.model.Checklist checklist) {
        Checklist entity = new Checklist();
        entity.setUserId(userId);
        entity.setTitle(checklist.getTitle());
        entity.setCreatedAt(OffsetDateTime.now());
        return mapper.toCreateChecklistResponse(toDto(checklistRepository.save(entity)));
    }

    /**
     * Updates the title of an existing checklist. Ownership is enforced before applying changes.
     *
     * @param userId    the ID of the authenticated user
     * @param id        the ID of the checklist to update
     * @param checklist the checklist data containing the updated title
     * @return a response containing the updated checklist
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if no checklist with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the checklist does not belong to the specified user
     */
    @Override
    public UpdateChecklistResponse updateChecklist(Long userId, Long id, org.openapitools.model.Checklist checklist) {
        Checklist entity = getOwnedChecklistEntity(userId, id);
        entity.setTitle(checklist.getTitle());
        return mapper.toUpdateChecklistResponse(toDto(checklistRepository.save(entity)));
    }

    /**
     * Deletes a checklist and cascade-deletes all its items via JPA. Ownership is enforced
     * before deletion.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the checklist to delete
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if no checklist with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the checklist does not belong to the specified user
     */
    @Override
    public void deleteChecklist(Long userId, Long id) {
        Checklist entity = getOwnedChecklistEntity(userId, id);
        checklistRepository.delete(entity);
    }

    /**
     * Adds a new item to a checklist. The item is persisted via its own repository (not through
     * the parent collection) to keep the operation lightweight. If the client omits
     * {@code position}, it defaults to one past the current item count.
     *
     * @param userId      the ID of the authenticated user
     * @param checklistId the ID of the checklist to add the item to
     * @param dto         the item data containing text, completion status, and optional position
     * @return a response containing the created item
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if no checklist with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the checklist does not belong to the specified user
     */
    @Override
    public AddChecklistItemResponse addChecklistItem(Long userId, Long checklistId, org.openapitools.model.ChecklistItem dto) {
        Checklist checklist = getOwnedChecklistEntity(userId, checklistId);
        ChecklistItem item = new ChecklistItem();
        item.setChecklist(checklist);
        item.setText(dto.getText());
        item.setCompleted(Boolean.TRUE.equals(dto.getCompleted()));
        item.setPosition(dto.getPosition() != null ? dto.getPosition() : checklist.getItems().size() + 1);
        return mapper.toAddChecklistItemResponse(toDto(checklistItemRepository.save(item)));
    }

    /**
     * Updates an existing checklist item. Performs an explicit cross-check that the item
     * actually belongs to the parent checklist (defence against malformed or stale client
     * references). The position is only overwritten when the request provides a non-null value.
     *
     * @param userId      the ID of the authenticated user
     * @param checklistId the ID of the checklist containing the item
     * @param itemId      the ID of the item to update
     * @param dto         the item data containing updated text, completion status, and optional position
     * @return a response containing the updated item
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if no checklist with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the checklist does not belong to the specified user
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException
     *         if no item with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException
     *         if the item does not belong to the specified checklist
     */
    @Override
    public UpdateChecklistItemResponse updateChecklistItem(Long userId, Long checklistId, Long itemId, org.openapitools.model.ChecklistItem dto) {
        getOwnedChecklistEntity(userId, checklistId);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ChecklistItemNotFoundException(itemId));
        if (!item.getChecklist().getId().equals(checklistId)) {
            throw new ChecklistItemNotInChecklistException(itemId, checklistId);
        }
        item.setText(dto.getText());
        item.setCompleted(Boolean.TRUE.equals(dto.getCompleted()));
        if (dto.getPosition() != null) {
            item.setPosition(dto.getPosition());
        }
        return mapper.toUpdateChecklistItemResponse(toDto(checklistItemRepository.save(item)));
    }

    /**
     * Deletes a checklist item. Verifies that the item belongs to the specified checklist
     * before deletion.
     *
     * @param userId      the ID of the authenticated user
     * @param checklistId the ID of the checklist containing the item
     * @param itemId      the ID of the item to delete
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException
     *         if no checklist with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.IllegalChecklistAccessException
     *         if the checklist does not belong to the specified user
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException
     *         if no item with the given ID exists
     * @throws de.tum.devopss26.checklistservice.exception.ChecklistItemNotInChecklistException
     *         if the item does not belong to the specified checklist
     */
    @Override
    public void deleteChecklistItem(Long userId, Long checklistId, Long itemId) {
        getOwnedChecklistEntity(userId, checklistId);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ChecklistItemNotFoundException(itemId));
        if (!item.getChecklist().getId().equals(checklistId)) {
            throw new ChecklistItemNotInChecklistException(itemId, checklistId);
        }
        checklistItemRepository.delete(item);
    }

    // Single ownership guard used by all checklist-level operations.
    private Checklist getOwnedChecklistEntity(Long userId, Long id) {
        Checklist entity = checklistRepository.findById(id)
                .orElseThrow(() -> new ChecklistNotFoundException(id));
        if (!entity.getUserId().equals(userId)) {
            throw new IllegalChecklistAccessException(userId, entity.getUserId(), id);
        }
        return entity;
    }

    private IdentifiedChecklist toDto(Checklist entity) {
        IdentifiedChecklist dto = new IdentifiedChecklist();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setTitle(entity.getTitle());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt());
        }
        dto.setItems(entity.getItems().stream().map(this::toDto).toList());
        return dto;
    }

    // Note: {@code completed} is a primitive boolean, so the DTO always carries a
    // value even when the entity has its default.
    private IdentifiedChecklistItem toDto(ChecklistItem entity) {
        IdentifiedChecklistItem dto = new IdentifiedChecklistItem();
        dto.setId(entity.getId());
        dto.setText(entity.getText());
        dto.setCompleted(entity.isCompleted());
        dto.setPosition(entity.getPosition());
        return dto;
    }
}
