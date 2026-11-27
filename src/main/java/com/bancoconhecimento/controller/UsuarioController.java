package com.bancoconhecimento.controller;

import com.bancoconhecimento.config.AutorizacaoInterceptor;
import com.bancoconhecimento.dto.Dtos.AtualizarUsuarioRequest;
import com.bancoconhecimento.dto.Dtos.CriarUsuarioRequest;
import com.bancoconhecimento.dto.Dtos.SenhaAdminRequest;
import com.bancoconhecimento.dto.Dtos.UsuarioItem;
import com.bancoconhecimento.model.Usuario;
import com.bancoconhecimento.service.Seguranca;
import com.bancoconhecimento.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestao de usuarios — SOMENTE o perfil ADMIN (direcao). */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios",
        description = "Criacao e gerenciamento de usuarios com perfis "
                + "(ADMIN, USER1, USER2) — acesso restrito ao ADMIN")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    private UsuarioItem de(Usuario u) {
        return new UsuarioItem(u.getId(), u.getLogin(), u.getNome(),
                u.getPerfil(), u.isDeveTrocarSenha());
    }

    @GetMapping
    @Operation(summary = "Listar usuarios (ADMIN)")
    public List<UsuarioItem> listar(HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN);
        return usuarios.listar().stream().map(this::de).toList();
    }

    @PostMapping
    @Operation(summary = "Criar usuario com perfil (ADMIN). "
            + "perfil: ADMIN, USER1 ou USER2")
    public UsuarioItem criar(@Valid @RequestBody CriarUsuarioRequest corpo,
                             HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN);
        return de(usuarios.criar(corpo.login(), corpo.senha(), corpo.nome(),
                corpo.perfil(), corpo.deveTrocarSenha()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar nome, senha e perfil (ADMIN). "
            + "Deixe a senha vazia para manter a atual")
    public UsuarioItem atualizar(@PathVariable Long id,
                                 @Valid @RequestBody AtualizarUsuarioRequest corpo,
                                 HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN);
        return de(usuarios.atualizar(id, corpo.nome(), corpo.senha(),
                corpo.perfil(), corpo.deveTrocarSenha()));
    }

    @PostMapping("/liberar-swagger")
    @Operation(summary = "Liberar o Swagger na sessao atual ao confirmar a senha do ADMIN")
    public ResponseEntity<?> liberarSwagger(
            @Valid @RequestBody SenhaAdminRequest corpo,
            HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN);
        Usuario admin = (Usuario) request.getSession()
                .getAttribute(AutorizacaoInterceptor.USUARIO_SESSAO);
        if (!usuarios.confirmarSenha(admin, corpo.senha()))
            return ResponseEntity.status(401).body(
                    java.util.Map.of("erro", "Senha de administrador invalida."));
        request.getSession().setAttribute(
                AutorizacaoInterceptor.SWAGGER_LIBERADO, true);
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuario (ADMIN). "
            + "Nao e possivel excluir a si mesmo nem o ultimo ADMIN")
    public ResponseEntity<?> remover(@PathVariable Long id,
                                     HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN);
        Usuario quemPede = (Usuario) request.getSession()
                .getAttribute(com.bancoconhecimento.config
                        .AutorizacaoInterceptor.USUARIO_SESSAO);
        usuarios.remover(id, quemPede);
        return ResponseEntity.ok(java.util.Map.of("ok", true, "id", id));
    }
}
