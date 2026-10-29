package com.moveinsync.distance.api;

import com.moveinsync.distance.api.dto.Dtos;
import com.moveinsync.distance.security.AuthProperties;
import com.moveinsync.distance.security.InvalidCredentialsException;
import com.moveinsync.distance.security.JwtService;
import com.moveinsync.distance.security.Role;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exchanges configured credentials for a signed JWT carrying the caller's tenant and role. In a
 * real deployment this would delegate to an identity provider; here it validates the demo users
 * from configuration.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthProperties props;
    private final JwtService jwt;

    public AuthController(AuthProperties props, JwtService jwt) {
        this.props = props;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public Dtos.LoginResponse login(@RequestBody Dtos.LoginRequest request) {
        AuthProperties.User user = props.users().stream()
                .filter(u -> u.username().equals(request.username()) && u.password().equals(request.password()))
                .findFirst()
                .orElseThrow(InvalidCredentialsException::new);
        String token = jwt.issue(user.username(), user.tenantId(), Role.valueOf(user.role()));
        return new Dtos.LoginResponse(token, user.tenantId(), user.role());
    }
}
