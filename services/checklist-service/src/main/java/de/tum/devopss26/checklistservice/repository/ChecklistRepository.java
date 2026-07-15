package de.tum.devopss26.checklistservice.repository;

import de.tum.devopss26.checklistservice.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    List<Checklist> findByUserId(Long userId);
}
