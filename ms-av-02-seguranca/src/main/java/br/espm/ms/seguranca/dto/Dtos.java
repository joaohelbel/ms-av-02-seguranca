package br.espm.ms.seguranca.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Objetos de entrada e saída dos endpoints (records = classes imutáveis com getters prontos).
 */
public final class Dtos {

    private Dtos() {
    }

    @Schema(name = "Login", description = "Credenciais para login")
    public record LoginRequest(
            @Schema(example = "joao") String username,
            @Schema(example = "123456") String senha) {
    }

    @Schema(name = "TrocaSenha", description = "Senha atual e nova senha (máx. 10 caracteres)")
    public record TrocaSenhaRequest(
            @Schema(example = "123456") String atual,
            @Schema(example = "novaSenha1") String nova) {
    }

    @Schema(name = "NovoUsuario", description = "Usuário a ser cadastrado (senha com máx. 10 caracteres)")
    public record NovoUsuarioRequest(
            @Schema(example = "carlos") String username,
            @Schema(example = "senha123") String senha) {
    }

    @Schema(name = "Usuario", description = "Usuário cadastrado e sua situação de bloqueio")
    public record UsuarioResponse(
            @Schema(example = "joao") String username,
            @Schema(example = "false") boolean bloqueado,
            @Schema(example = "0") int tentativasFalhas) {
    }

    @Schema(name = "Mensagem", description = "Resposta simples com mensagem")
    public record Mensagem(String mensagem) {
    }

}
