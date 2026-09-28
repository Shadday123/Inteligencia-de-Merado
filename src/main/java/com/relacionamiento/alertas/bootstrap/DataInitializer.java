package com.relacionamiento.alertas.bootstrap;

import com.relacionamiento.alertas.domain.Role;
import com.relacionamiento.alertas.domain.Usuario;
import com.relacionamiento.alertas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() == 0) {
            String encryptedPassword = passwordEncoder.encode("Seguridad123!");

            List<Usuario> usuariosIniciales = List.of(
                    // Administradores (Pueden borrar, cambiar estado, etc.)
                    new Usuario("admin.escuela", encryptedPassword, "Administrador Principal", Role.ADMIN),
                    new Usuario("coord.relaciones", encryptedPassword, "Coordinador Relacionamiento", Role.ADMIN),
                    
                    // Usuarios de lectura (Profesores, directivos, consultores)
                    new Usuario("dir.academico", encryptedPassword, "Director Académico", Role.USER),
                    new Usuario("profesor.titular", encryptedPassword, "Profesor Titular", Role.USER),
                    new Usuario("consultor.ext", encryptedPassword, "Consultor Externo", Role.USER),
                    new Usuario("asesor.proyectos", encryptedPassword, "Asesor de Proyectos", Role.USER),
                    new Usuario("analista.datos", encryptedPassword, "Analista de Datos", Role.USER),
                    new Usuario("asistente.dir", encryptedPassword, "Asistente de Dirección", Role.USER),
                    new Usuario("gestor.alianzas", encryptedPassword, "Gestor de Alianzas", Role.USER),
                    new Usuario("usuario.prueba", encryptedPassword, "Usuario de Prueba", Role.USER)
            );

            usuarioRepository.saveAll(usuariosIniciales);
            System.out.println("=========================================================");
            System.out.println("✅ Se han creado 10 credenciales iniciales en el sistema.");
            System.out.println("🔑 Ejemplo de Admin -> User: admin.escuela | Pass: Seguridad123!");
            System.out.println("🔑 Ejemplo de Lector -> User: usuario.prueba | Pass: Seguridad123!");
            System.out.println("=========================================================");
        }
    }
}
