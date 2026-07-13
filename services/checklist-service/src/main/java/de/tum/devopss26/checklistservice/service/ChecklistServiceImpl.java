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

@Service
@RequiredArgsConstructor
@Transactional
public class ChecklistServiceImpl implements ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChecklistMapper mapper;

    @Override
    public GetChecklistsResponse getChecklists(Long userId) {
        return mapper.toGetChecklistsResponse(checklistRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList());
    }

    @Override
    public GetChecklistResponse getChecklistById(Long userId, Long id) {
        ChecklistEntity entity = getOwnedChecklistEntity(userId, id);
        return mapper.toGetChecklistResponse(toDto(entity));
    }

    @Override
    public CreateChecklistResponse createChecklist(Long userId, Checklist dto) {
        ChecklistEntity entity = new ChecklistEntity();
        entity.setUserId(userId);
        entity.setTitle(dto.getTitle());
        entity.setCreatedAt(OffsetDateTime.now());
        return mapper.toCreateChecklistResponse(toDto(checklistRepository.save(entity)));
    }

    @Override
    public UpdateChecklistResponse updateChecklist(Long userId, Long id, Checklist dto) {
        ChecklistEntity entity = getOwnedChecklistEntity(userId, id);
        entity.setTitle(dto.getTitle());
        return mapper.toUpdateChecklistResponse(toDto(checklistRepository.save(entity)));
    }

    @Override
    public void deleteChecklist(Long userId, Long id) {
        ChecklistEntity entity = getOwnedChecklistEntity(userId, id);
        checklistRepository.delete(entity);
    }

    @Override
    public AddChecklistItemResponse addChecklistItem(Long userId, Long checklistId, ChecklistItem dto) {
        ChecklistEntity checklist = getOwnedChecklistEntity(userId, checklistId);
        ChecklistItemEntity item = new ChecklistItemEntity();
        item.setChecklist(checklist);
        item.setText(dto.getText());
        item.setCompleted(Boolean.TRUE.equals(dto.getCompleted()));
        item.setPosition(dto.getPosition() != null ? dto.getPosition() : checklist.getItems().size() + 1);
        return mapper.toAddChecklistItemResponse(toDto(checklistItemRepository.save(item)));
    }

    @Override
    public UpdateChecklistItemResponse updateChecklistItem(Long userId, Long checklistId, Long itemId, ChecklistItem dto) {
        getOwnedChecklistEntity(userId, checklistId);
        ChecklistItemEntity item = checklistItemRepository.findById(itemId)
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

    private ChecklistEntity getOwnedChecklistEntity(Long userId, Long id) {
        ChecklistEntity entity = checklistRepository.findById(id)
                .orElseThrow(() -> new ChecklistNotFoundException(id));
        if (!entity.getUserId().equals(userId)) {
            throw new IllegalChecklistAccessException(userId, entity.getUserId(), id);
        }
        return entity;
    }

    private IdentifiedChecklist toDto(ChecklistEntity entity) {
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

    private IdentifiedChecklistItem toDto(ChecklistItemEntity entity) {
        IdentifiedChecklistItem dto = new IdentifiedChecklistItem();
        dto.setId(entity.getId());
        dto.setText(entity.getText());
        dto.setCompleted(entity.isCompleted());
        dto.setPosition(entity.getPosition());
        return dto;
    }
}
