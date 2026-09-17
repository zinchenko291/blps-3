package me.zinch.itmo.mts.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.zinch.itmo.mts.security.JaasXmlAuthenticationProvider;
import me.zinch.itmo.mts.security.JwtService;
import me.zinch.itmo.mts.security.SecurityAccount;
import me.zinch.itmo.mts.web.auth.dto.AuthUserResponse;
import me.zinch.itmo.mts.web.auth.dto.LoginRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JaasXmlAuthenticationProvider jaasAuthenticationProvider;
    private final JwtService jwtService;

    public AuthController(
            JaasXmlAuthenticationProvider jaasAuthenticationProvider,
            JwtService jwtService) {
        this.jaasAuthenticationProvider = jaasAuthenticationProvider;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthUserResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = jaasAuthenticationProvider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getLogin(),
                        request.getPassword()));
        SecurityAccount account = (SecurityAccount) authentication.getPrincipal();
        return new AuthUserResponse(account.id(), account.login(), account.name(), account.role(),
                jwtService.issue(account));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal SecurityAccount account) {
        return new AuthUserResponse(account.id(), account.login(), account.name(), account.role(), null);
    }
}
