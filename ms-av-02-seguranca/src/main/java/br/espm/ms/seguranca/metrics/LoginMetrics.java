package br.espm.ms.seguranca.metrics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Indicadores expostos em /actuator/prometheus:
 *   login_validos_total    (counter)
 *   login_invalidos_total  (counter)
 *   usuarios_bloqueados    (gauge, lido da tabela login_attempts)
 */
@Component
public class LoginMetrics {

    private final Counter loginsValidos;
    private final Counter loginsInvalidos;

    public LoginMetrics(MeterRegistry registry, JdbcTemplate jdbc) {

        this.loginsValidos = Counter.builder("login.validos")
                .description("Quantidade de logins válidos")
                .register(registry);

        this.loginsInvalidos = Counter.builder("login.invalidos")
                .description("Quantidade de logins inválidos")
                .register(registry);

        Gauge.builder("usuarios.bloqueados", jdbc, LoginMetrics::contarBloqueados)
                .description("Total de usuários bloqueados")
                .register(registry);
    }

    private static double contarBloqueados(JdbcTemplate jdbc) {
        try {
            Integer total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM login_attempts WHERE locked = TRUE", Integer.class);
            return total == null ? 0 : total;
        } catch (Exception e) {
            return 0;
        }
    }

    public void registrarValido() {
        loginsValidos.increment();
    }

    public void registrarInvalido() {
        loginsInvalidos.increment();
    }

}
