package me.zinch.itmo.mts.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final XmlAccountStore store;

    public JwtAuthenticationFilter(JwtService jwt, XmlAccountStore store) {
        this.jwt = jwt;
        this.store = store;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer "))
            try {
                SecurityAccount securityAccount = jwt.parse(header.substring(7), store);
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                UsernamePasswordAuthenticationToken.authenticated(
                                        securityAccount,
                                        null,
                                        RolePrivileges.authorities(securityAccount)));
            } catch (RuntimeException ignored) {
            }
        chain.doFilter(request, response);
    }
}
