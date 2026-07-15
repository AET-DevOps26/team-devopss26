package de.tum.devopss26.userservice.mapper;

import de.tum.devopss26.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Uses {@code componentModel = "spring"} so MapStruct generates a Spring bean that can
 * be injected wherever needed.
 *
 * <p><b>Password hashing:</b>
 * The {@link PasswordEncoder} is injected via a dedicated setter with {@code @Autowired}
 * rather than through the constructor because MapStruct cannot automatically inject beans
 * into abstract mapper classes through a constructor — it needs a no-arg constructor to
 * create the proxy. The setter is called by Spring after instantiation.
 *
 * <p><b>Mapping details:</b>
 * <ul>
 *   <li>{@code password → passwordHash}: The plain-text password from the request is hashed
 *       via the {@code hashPassword} named method before being stored. This means the entity
 *       never holds a plain-text password at any point.</li>
 *   <li>{@code id → ignored}: The {@code id} field is database-generated ({@code IDENTITY}),
 *       so it must not be set from the request DTO.</li>
 * </ul>
 */
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

    /**
     * The null guard exists because MapStruct may pass null when the source field is null;
     * in practice the registration request should always have a non-null password, but
     * we handle it defensively to avoid {@code NullPointerException} in the encoder.
     */
    @Named("hashPassword")
    protected String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        return passwordEncoder.encode(password);
    }
}
