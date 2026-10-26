package br.espm.ms.seguranca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

/**
 * Cria os usuários com role usando a API do JdbcUserDetailsManager.
 *
 *   admin      / admin123  -> ROLE_ADMIN       (PUT /unlock/{userid})
 *   manutencao / manut123  -> ROLE_MANUTENCAO  (POST / e GET /)
 */
@Component
public class CargaUsuariosRoles implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CargaUsuariosRoles.class);

    private final JdbcUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public CargaUsuariosRoles(JdbcUserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        criar("admin", "admin123", "ADMIN");
        criar("manutencao", "manut123", "MANUTENCAO");
    }

    private void criar(String username, String senha, String role) {
        if (userDetailsManager.userExists(username)) {
            return;
        }
        userDetailsManager.createUser(User.withUsername(username)
                .password(passwordEncoder.encode(senha))
                .roles(role)
                .build());
        logger.info("Usuário {} criado com ROLE_{}", username, role);
    }

}
