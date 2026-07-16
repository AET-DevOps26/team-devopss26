package de.tum.devopss26.checklistservice.repository;

import de.tum.devopss26.checklistservice.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Checklist} entities.
 * Provides standard CRUD operations and custom queries for checklist management.
 */
@Repository
public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    /**
     * Finds all checklists owned by the specified user.
     *
     * @param userId the ID of the user whose checklists to retrieve
     * @return a list of checklists belonging to the given user
     */
    List<Checklist> findByUserId(Long userId);
}
