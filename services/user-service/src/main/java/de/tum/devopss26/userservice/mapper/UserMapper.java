package de.tum.devopss26.userservice.mapper;

import de.tum.devopss26.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Mapping(source = "password", target = "passwordHash", qualifiedByName = "hashPassword")
    @Mapping(target = "id", ignore = true)
    public abstract User toEntity(RegisterUserRequest request);

    @Named("hashPassword")
    protected String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        return passwordEncoder.encode(password);
    }
}
