package de.tum.devopss26.userservice.repository;

import de.tum.devopss26.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByUsername(String username);
	Optional<User> findByUsernameIgnoreCase(String username);
	
}
