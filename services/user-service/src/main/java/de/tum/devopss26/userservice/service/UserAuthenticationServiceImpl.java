package de.tum.devopss26.userservice.service;

import de.tum.devopss26.userservice.entity.User;
import de.tum.devopss26.userservice.exception.UserAlreadyExistsException;
import de.tum.devopss26.userservice.mapper.UserMapper;
import de.tum.devopss26.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class UserAuthenticationServiceImpl implements UserAuthenticationService {

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

        Optional<User> opt = repository.findByUsernameIgnoreCase(username);
        if (opt.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }

        return jwtService.generateToken(opt.get().getId(), opt.get().getUsername());
    }

    @Override
    public boolean checkToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }
            String jwt = authHeader.substring(7);
            return jwtService.isTokenValid(jwt);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error checking auth token");
            return false;
        }
    }
}
