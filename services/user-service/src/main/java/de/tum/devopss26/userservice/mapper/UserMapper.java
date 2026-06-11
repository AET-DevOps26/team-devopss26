package de.tum.devopss26.userservice.mapper;

import de.tum.devopss26.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    private final PasswordEncoder passwordEncoder;

    // Maps the plain text 'password' from CreateUserRequest to 'passwordHash' in the User entity
    @Mapping(source = "password", target = "passwordHash")
    @Mapping(target = "id", ignore = true)
    public abstract User toEntity(RegisterUserRequest request);

    // Custom helper method that MapStruct automatically calls to hash the password string
    protected String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        return passwordEncoder.encode(password);
    }
}
