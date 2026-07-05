package de.tum.devopss26.checklistservice.service;

import de.tum.devopss26.checklistservice.entity.ChecklistEntity;
import de.tum.devopss26.checklistservice.entity.ChecklistItemEntity;
import de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException;
import de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException;
import de.tum.devopss26.checklistservice.repository.ChecklistItemRepository;
import de.tum.devopss26.checklistservice.repository.ChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChecklistServiceImpl implements ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;

    @Override
    public List<Checklist> getChecklists(Long userId) {
        return checklistRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Checklist getChecklistById(Long id) {
        ChecklistEntity entity = checklistRepository.findById(id)
                .orElseThrow(() -> new ChecklistNotFoundException(id));
        return toDto(entity);
    }

    @Override
    public Checklist createChecklist(Long userId, Checklist dto) {
        ChecklistEntity entity = new ChecklistEntity();
        entity.setUserId(userId);
        entity.setTitle(dto.getTitle());
        entity.setCreatedAt(LocalDateTime.now());
        return toDto(checklistRepository.save(entity));
    }

    @Override
    public Checklist updateChecklist(Long id, Checklist dto) {
        ChecklistEntity entity = checklistRepository.findById(id)
                .orElseThrow(() -> new ChecklistNotFoundException(id));
        entity.setTitle(dto.getTitle());
        return toDto(checklistRepository.save(entity));
    }

    @Override
    public void deleteChecklist(Long id) {
        if (!checklistRepository.existsById(id)) {
            throw new ChecklistNotFoundException(id);
        }
        checklistRepository.deleteById(id);
    }

    @Override
    public ChecklistItem addChecklistItem(Long checklistId, ChecklistItem dto) {
        ChecklistEntity checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new ChecklistNotFoundException(checklistId));
        ChecklistItemEntity item = new ChecklistItemEntity();
        item.setChecklist(checklist);
        item.setText(dto.getText());
        item.setCompleted(Boolean.TRUE.equals(dto.getCompleted()));
        item.setPosition(dto.getPosition() != null ? dto.getPosition() : checklist.getItems().size() + 1);
        return toDto(checklistItemRepository.save(item));
    }

    @Override
    public ChecklistItem updateChecklistItem(Long checklistId, Long itemId, ChecklistItem dto) {
        ChecklistItemEntity item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ChecklistItemNotFoundException(itemId));
        if (!item.getChecklist().getId().equals(checklistId)) {
            throw new ChecklistItemNotFoundException(itemId);
        }
        item.setText(dto.getText());
        item.setCompleted(Boolean.TRUE.equals(dto.getCompleted()));
        if (dto.getPosition() != null) {
            item.setPosition(dto.getPosition());
        }
        return toDto(checklistItemRepository.save(item));
    }

    @Override
    public void deleteChecklistItem(Long checklistId, Long itemId) {
        ChecklistItemEntity item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ChecklistItemNotFoundException(itemId));
        if (!item.getChecklist().getId().equals(checklistId)) {
            throw new ChecklistItemNotFoundException(itemId);
        }
        checklistItemRepository.delete(item);
    }

    private Checklist toDto(ChecklistEntity entity) {
        Checklist dto = new Checklist();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setTitle(entity.getTitle());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(OffsetDateTime.of(entity.getCreatedAt(), ZoneOffset.UTC));
        }
        dto.setItems(entity.getItems().stream().map(this::toDto).toList());
        return dto;
    }

    private ChecklistItem toDto(ChecklistItemEntity entity) {
        ChecklistItem dto = new ChecklistItem();
        dto.setId(entity.getId());
        dto.setText(entity.getText());
        dto.setCompleted(entity.isCompleted());
        dto.setPosition(entity.getPosition());
        return dto;
    }
}
