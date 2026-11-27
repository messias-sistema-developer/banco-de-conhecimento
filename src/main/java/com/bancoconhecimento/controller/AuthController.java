package com.bancoconhecimento.controller;

import com.bancoconhecimento.config.AutorizacaoInterceptor;
import com.bancoconhecimento.dto.Dtos.LoginRequest;
import com.bancoconhecimento.dto.Dtos.TrocarSenhaRequest;
import com.bancoconhecimento.dto.Dtos.UsuarioResponse;
import com.bancoconhecimento.model.Usuario;
import com.bancoconhecimento.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao", description = "Login e logout da tela web")
public class AuthController {

    private final UsuarioService usuarios;

    public AuthController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar com login e senha (cria a sessao)")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest corpo,
                                    HttpServletRequest request) {
        Usuario usuario = usuarios.autenticar(corpo.login(), corpo.senha());
        if (usuario == null) {
            return ResponseEntity.status(401).body(
                    Map.of("erro", "Login ou senha invalidos."));
        }
        HttpSession sessao = request.getSession(true);
        sessao.setAttribute(AutorizacaoInterceptor.USUARIO_SESSAO, usuario);
        return ResponseEntity.ok(new UsuarioResponse(usuario.getLogin(),
                usuario.getNome(), usuario.getPerfil(),
                usuario.isDeveTrocarSenha()));
    }

    @PostMapping("/trocar-senha")
    @Operation(summary = "Trocar a senha temporaria pela nova senha do usuario")
    public ResponseEntity<?> trocarSenha(
            @Valid @RequestBody TrocarSenhaRequest corpo,
            HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        Object atributo = sessao == null ? null
                : sessao.getAttribute(AutorizacaoInterceptor.USUARIO_SESSAO);
        if (!(atributo instanceof Usuario usuario)) {
            return ResponseEntity.status(401).body(
                    Map.of("erro", "Ninguem autenticado nesta sessao."));
        }
        Usuario atualizado = usuarios.trocarSenha(usuario,
                corpo.senhaAtual(), corpo.novaSenha());
        sessao.setAttribute(AutorizacaoInterceptor.USUARIO_SESSAO, atualizado);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar a sessao")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) sessao.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario logado na sessao atual")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        Object usuario = sessao == null ? null
                : sessao.getAttribute(AutorizacaoInterceptor.USUARIO_SESSAO);
        if (usuario instanceof Usuario u) {
            return ResponseEntity.ok(new UsuarioResponse(u.getLogin(),
                    u.getNome(), u.getPerfil(), u.isDeveTrocarSenha()));
        }
        return ResponseEntity.status(401).body(
                Map.of("erro", "Ninguem autenticado nesta sessao."));
    }
}
