package de.tum.devopss26.userservice.service;

import de.tum.devopss26.userservice.entity.User;
import de.tum.devopss26.userservice.exception.UserAlreadyExistsException;
import de.tum.devopss26.userservice.mapper.UserMapper;
import de.tum.devopss26.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthenticationServiceImpl implements UserAuthenticationService {

	private final UserRepository repository;
	private final UserMapper mapper;

	@Override
	public void registerUser(RegisterUserRequest request) {
		if (repository.existsByUsername(request.getUsername())) {
			throw new UserAlreadyExistsException(request.getUsername());
		}

		User mapped = mapper.toEntity(request);
		repository.save(mapped);
	}
}
