package de.tum.devopss26.userservice.controller;

import de.tum.devopss26.userservice.service.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
public class PublicKeyController {

    private final JwtService jwtService;

    public PublicKeyController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/api/v1/users/auth/public-key")
    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(jwtService.getPublicKey().getEncoded());
    }
}
