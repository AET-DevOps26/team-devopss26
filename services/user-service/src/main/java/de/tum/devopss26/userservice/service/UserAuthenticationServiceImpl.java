package de.tum.devopss26.userservice.service;

import de.tum.devopss26.userservice.entity.User;
import de.tum.devopss26.userservice.exception.UserAlreadyExistsException;
import de.tum.devopss26.userservice.mapper.UserMapper;
import de.tum.devopss26.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthenticationServiceImpl implements UserAuthenticationService {

	private final UserRepository repository;
	private final UserMapper mapper;
	private final JwtService jwtService;

	@Override
	public void registerUser(RegisterUserRequest request) {
		if (repository.existsByUsername(request.getUsername())) {
			throw new UserAlreadyExistsException(request.getUsername());
		}

		User mapped = mapper.toEntity(request);
		repository.save(mapped);
	}

	@Override
	public String loginUser() {
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		return jwtService.generateToken(username);
	}

	@Override
	public boolean checkToken(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return false;
		}
		String jwt = authHeader.substring(7);
		String username = jwtService.extractUsername(jwt);
		return username != null && jwtService.isTokenValid(jwt, username);
	}
}
