package com.relacionamiento.alertas.service;

import com.relacionamiento.alertas.domain.Usuario;
import com.relacionamiento.alertas.dto.auth.AuthRequest;
import com.relacionamiento.alertas.dto.auth.AuthResponse;
import com.relacionamiento.alertas.repository.UsuarioRepository;
import com.relacionamiento.alertas.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository repository, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        
        Usuario user = repository.findByUsername(request.getUsername())
                .orElseThrow();
                
        String jwtToken = jwtService.generateToken(user);
        
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name());
    }
}
