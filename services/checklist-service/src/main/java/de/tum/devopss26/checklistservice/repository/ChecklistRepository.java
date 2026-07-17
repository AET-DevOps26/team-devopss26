package de.tum.devopss26.checklistservice.repository;

import de.tum.devopss26.checklistservice.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Checklist} entities.
 */
@Repository
public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    /**
     * Finds all checklists belonging to the specified user.
     *
     * @param userId the ID of the user whose checklists to find
     * @return a list of checklists owned by the user, or an empty list if none exist
     */
    List<Checklist> findByUserId(Long userId);
}
