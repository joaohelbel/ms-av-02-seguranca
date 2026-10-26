package br.espm.ms.seguranca.service;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.espm.ms.seguranca.dto.Dtos.UsuarioResponse;
import br.espm.ms.seguranca.metrics.LoginMetrics;

@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    /** "Caso o usuário falhe mais de uma vez no login ele deve ser bloqueado" */
    private static final int MAX_FALHAS_PERMITIDAS = 1;

    /** Limite da coluna usuarios.senha VARCHAR(10) */
    private static final int TAMANHO_MAX_SENHA = 10;

    public enum ResultadoLogin { SUCESSO, INVALIDO, BLOQUEADO }

    public enum ResultadoTrocaSenha { SUCESSO, NAO_ENCONTRADO, SENHA_ATUAL_INCORRETA, BLOQUEADO, NOVA_INVALIDA }

    public enum ResultadoCadastro { CRIADO, JA_EXISTE, DADOS_INVALIDOS }

    private final JdbcTemplate jdbc;
    private final LoginMetrics metrics;

    public UsuarioService(JdbcTemplate jdbc, LoginMetrics metrics) {
        this.jdbc = jdbc;
        this.metrics = metrics;
    }

    // ------------------------------------------------------------------ login

    @Transactional
    public ResultadoLogin login(String username, String senha) {

        List<String> senhaBanco = buscarSenha(username);

        if (senhaBanco.isEmpty()) {
            logger.info("Login inválido: usuário {} não existe", username);
            metrics.registrarInvalido();
            return ResultadoLogin.INVALIDO;
        }

        garantirControleTentativas(username);

        if (estaBloqueado(username)) {
            logger.info("Login negado: usuário {} está bloqueado", username);
            metrics.registrarInvalido();
            return ResultadoLogin.BLOQUEADO;
        }

        if (Objects.equals(senhaBanco.get(0), senha)) {
            jdbc.update("UPDATE login_attempts SET failed_attempts = 0, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE username = ?", username);
            metrics.registrarValido();
            logger.info("Login válido: {}", username);
            return ResultadoLogin.SUCESSO;
        }

        jdbc.update("UPDATE login_attempts SET failed_attempts = failed_attempts + 1, "
                + "last_failed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE username = ?", username);
        metrics.registrarInvalido();

        Integer falhas = jdbc.queryForObject(
                "SELECT failed_attempts FROM login_attempts WHERE username = ?", Integer.class, username);

        if (falhas != null && falhas > MAX_FALHAS_PERMITIDAS) {
            jdbc.update("UPDATE login_attempts SET locked = TRUE, locked_at = CURRENT_TIMESTAMP, "
                    + "updated_at = CURRENT_TIMESTAMP WHERE username = ?", username);
            logger.info("Usuário {} bloqueado após {} falhas", username, falhas);
            return ResultadoLogin.BLOQUEADO;
        }

        logger.info("Login inválido: senha incorreta para {} ({} falha(s))", username, falhas);
        return ResultadoLogin.INVALIDO;
    }

    // ------------------------------------------------------------ troca senha

    @Transactional
    public ResultadoTrocaSenha trocarSenha(String username, String atual, String nova) {

        List<String> senhaBanco = buscarSenha(username);
        if (senhaBanco.isEmpty()) {
            return ResultadoTrocaSenha.NAO_ENCONTRADO;
        }

        garantirControleTentativas(username);
        if (estaBloqueado(username)) {
            return ResultadoTrocaSenha.BLOQUEADO;
        }

        if (!Objects.equals(senhaBanco.get(0), atual)) {
            return ResultadoTrocaSenha.SENHA_ATUAL_INCORRETA;
        }

        if (!senhaValida(nova)) {
            return ResultadoTrocaSenha.NOVA_INVALIDA;
        }

        jdbc.update("UPDATE usuarios SET senha = ? WHERE username = ?", nova, username);
        logger.info("Senha alterada para {}", username);
        return ResultadoTrocaSenha.SUCESSO;
    }

    // --------------------------------------------------------------- cadastro

    @Transactional
    public ResultadoCadastro cadastrar(String username, String senha) {

        if (username == null || username.isBlank() || username.length() > 100 || !senhaValida(senha)) {
            return ResultadoCadastro.DADOS_INVALIDOS;
        }

        if (!buscarSenha(username).isEmpty()) {
            return ResultadoCadastro.JA_EXISTE;
        }

        jdbc.update("INSERT INTO usuarios (username, senha) VALUES (?, ?)", username, senha);
        garantirControleTentativas(username);
        logger.info("Usuário {} cadastrado", username);
        return ResultadoCadastro.CRIADO;
    }

    // ---------------------------------------------------------------- unlock

    @Transactional
    public boolean desbloquear(String username) {

        if (buscarSenha(username).isEmpty()) {
            return false;
        }

        garantirControleTentativas(username);
        jdbc.update("UPDATE login_attempts SET locked = FALSE, failed_attempts = 0, locked_at = NULL, "
                + "updated_at = CURRENT_TIMESTAMP WHERE username = ?", username);
        logger.info("Usuário {} desbloqueado", username);
        return true;
    }

    // ---------------------------------------------------------------- listar

    public List<UsuarioResponse> listar() {
        return jdbc.query("SELECT u.username, COALESCE(a.locked, FALSE) AS locked, "
                + "COALESCE(a.failed_attempts, 0) AS failed_attempts "
                + "FROM usuarios u LEFT JOIN login_attempts a ON a.username = u.username "
                + "ORDER BY u.username",
                (rs, i) -> new UsuarioResponse(
                        rs.getString("username"),
                        rs.getBoolean("locked"),
                        rs.getInt("failed_attempts")));
    }

    // --------------------------------------------------------------- helpers

    private List<String> buscarSenha(String username) {
        return jdbc.query("SELECT senha FROM usuarios WHERE username = ?",
                (rs, i) -> rs.getString("senha"), username);
    }

    private void garantirControleTentativas(String username) {
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_attempts WHERE username = ?", Integer.class, username);
        if (existe == null || existe == 0) {
            jdbc.update("INSERT INTO login_attempts (username) VALUES (?)", username);
        }
    }

    private boolean estaBloqueado(String username) {
        Boolean locked = jdbc.queryForObject(
                "SELECT locked FROM login_attempts WHERE username = ?", Boolean.class, username);
        return Boolean.TRUE.equals(locked);
    }

    private boolean senhaValida(String senha) {
        return senha != null && !senha.isBlank() && senha.length() <= TAMANHO_MAX_SENHA;
    }

}
