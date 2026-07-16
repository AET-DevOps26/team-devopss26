package de.tum.devopss26.checklistservice.repository;

import de.tum.devopss26.checklistservice.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ChecklistItem} entities.
 */
@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
}
