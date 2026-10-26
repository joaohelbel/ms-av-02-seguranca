package br.espm.ms.seguranca.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Login Service - MS-AV-02",
        version = "1.0",
        description = "Login, troca de senha, cadastro, desbloqueio e listagem de usuários. "
                + "Endpoints protegidos usam Basic Auth (admin/admin123 ou manutencao/manut123)."))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfig {
}
