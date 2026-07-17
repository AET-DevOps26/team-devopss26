package de.tum.devopss26.userservice.repository;

import de.tum.devopss26.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * Checks whether a user with the given username already exists.
	 *
	 * @param username the username to check
	 * @return {@code true} if a user with the given username exists, {@code false} otherwise
	 */
	boolean existsByUsername(String username);

	/**
	 * Finds a user by username using a case-insensitive search.
	 *
	 * @param username the username to search for
	 * @return an {@link Optional} containing the user if found, or empty if not found
	 */
	Optional<User> findByUsernameIgnoreCase(String username);
	
}
