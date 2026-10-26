package br.espm.ms.seguranca.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.espm.ms.seguranca.dto.Dtos.LoginRequest;
import br.espm.ms.seguranca.dto.Dtos.Mensagem;
import br.espm.ms.seguranca.dto.Dtos.NovoUsuarioRequest;
import br.espm.ms.seguranca.dto.Dtos.TrocaSenhaRequest;
import br.espm.ms.seguranca.dto.Dtos.UsuarioResponse;
import br.espm.ms.seguranca.service.UsuarioService;
import br.espm.ms.seguranca.service.UsuarioService.ResultadoLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Usuários", description = "Login, troca de senha, cadastro, desbloqueio e listagem")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Efetua login",
            description = "Público. Valida usuário e senha na tabela usuarios. "
                    + "Na segunda falha o usuário é bloqueado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário e senha válidos",
                    content = @Content(schema = @Schema(implementation = Mensagem.class))),
            @ApiResponse(responseCode = "401", description = "Usuário e senha inválidos ou usuário bloqueado",
                    content = @Content(schema = @Schema(implementation = Mensagem.class)))
    })
    public ResponseEntity<Mensagem> login(@RequestBody LoginRequest req) {

        ResultadoLogin resultado = service.login(req.username(), req.senha());

        return switch (resultado) {
            case SUCESSO -> ResponseEntity.ok(new Mensagem("Login efetuado com sucesso"));
            case BLOQUEADO -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Mensagem("Usuário bloqueado"));
            case INVALIDO -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Mensagem("Usuário ou senha inválidos"));
        };
    }

    @PutMapping("/trocarsenha/{userid}")
    @Operation(summary = "Troca a senha do usuário",
            description = "Público. Confere a senha atual na base antes de gravar a nova (máx. 10 caracteres).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada"),
            @ApiResponse(responseCode = "400", description = "Nova senha inválida"),
            @ApiResponse(responseCode = "401", description = "Senha atual incorreta"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "423", description = "Usuário bloqueado")
    })
    public ResponseEntity<Mensagem> trocarSenha(
            @Parameter(description = "username do usuário", example = "joao") @PathVariable String userid,
            @RequestBody TrocaSenhaRequest req) {

        return switch (service.trocarSenha(userid, req.atual(), req.nova())) {
            case SUCESSO -> ResponseEntity.ok(new Mensagem("Senha alterada com sucesso"));
            case NOVA_INVALIDA -> ResponseEntity.badRequest()
                    .body(new Mensagem("Nova senha inválida (obrigatória, até 10 caracteres)"));
            case SENHA_ATUAL_INCORRETA -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Mensagem("Senha atual incorreta"));
            case NAO_ENCONTRADO -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Mensagem("Usuário não encontrado"));
            case BLOQUEADO -> ResponseEntity.status(HttpStatus.LOCKED)
                    .body(new Mensagem("Usuário bloqueado"));
        };
    }

    @PostMapping("/")
    @Operation(summary = "Cadastra um novo usuário",
            description = "Somente ROLE_MANUTENCAO (manutencao / manut123).",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem a role ROLE_MANUTENCAO"),
            @ApiResponse(responseCode = "409", description = "Usuário já existe")
    })
    public ResponseEntity<Mensagem> cadastrar(@RequestBody NovoUsuarioRequest req) {

        return switch (service.cadastrar(req.username(), req.senha())) {
            case CRIADO -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(new Mensagem("Usuário cadastrado"));
            case JA_EXISTE -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Mensagem("Usuário já existe"));
            case DADOS_INVALIDOS -> ResponseEntity.badRequest()
                    .body(new Mensagem("username obrigatório e senha com até 10 caracteres"));
        };
    }

    @PutMapping("/unlock/{userid}")
    @Operation(summary = "Desbloqueia um usuário",
            description = "Somente ROLE_ADMIN (admin / admin123).",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário desbloqueado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem a role ROLE_ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<Mensagem> desbloquear(
            @Parameter(description = "username do usuário", example = "joao") @PathVariable String userid) {

        if (service.desbloquear(userid)) {
            return ResponseEntity.ok(new Mensagem("Usuário desbloqueado"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Mensagem("Usuário não encontrado"));
    }

    @GetMapping("/")
    @Operation(summary = "Lista todos os usuários",
            description = "Somente ROLE_MANUTENCAO (manutencao / manut123).",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem a role ROLE_MANUTENCAO")
    })
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

}
