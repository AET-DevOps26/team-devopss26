package de.tum.devopss26.checklistservice.repository;

import de.tum.devopss26.checklistservice.entity.ChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Long> {

    List<ChecklistEntity> findByUserId(Long userId);
}
