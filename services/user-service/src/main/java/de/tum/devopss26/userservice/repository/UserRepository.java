package de.tum.devopss26.userservice.repository;

import de.tum.devopss26.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByUsername(String username);
	
}
