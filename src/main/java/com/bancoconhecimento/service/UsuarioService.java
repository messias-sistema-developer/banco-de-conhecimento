package com.bancoconhecimento.service;

import com.bancoconhecimento.model.Usuario;
import com.bancoconhecimento.exception.RequisicaoInvalidaException;
import com.bancoconhecimento.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/** Cria os usuarios dos 3 perfis na primeira execucao:
 *  ADMIN (direcao), USER1 (administrativo) e USER2 (operacional). */
@Service
public class UsuarioService implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();

    @Value("${app.admin-login:admin}")
    private String adminLogin;

    @Value("${app.admin-senha:admin123}")
    private String adminSenha;

    @Value("${app.user1-login:user1}")
    private String user1Login;

    @Value("${app.user1-senha:user123}")
    private String user1Senha;

    @Value("${app.user2-login:user2}")
    private String user2Login;

    @Value("${app.user2-senha:user123}")
    private String user2Senha;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public void run(ApplicationArguments args) {
        criarSeNaoExistir(adminLogin, adminSenha, "Direção",
                Seguranca.PERFIL_ADMIN);
        criarSeNaoExistir(user1Login, user1Senha, "Administrativo",
                Seguranca.PERFIL_USER1);
        criarSeNaoExistir(user2Login, user2Senha, "Operacional",
                Seguranca.PERFIL_USER2);
    }

    private void criarSeNaoExistir(String login, String senha, String nome,
                                   String perfil) {
        if (!usuarios.existsByLogin(login)) {
            usuarios.save(new Usuario(login, codificador.encode(senha),
                    nome, perfil));
        }
    }

    /** Retorna o usuario se a senha conferir, senao null. */
    public Usuario autenticar(String login, String senha) {
        return usuarios.findByLogin(login)
                .filter(u -> codificador.matches(senha, u.getSenhaHash()))
                .orElse(null);
    }

    // ------------------------------------------------ gestao (so ADMIN)

    public List<Usuario> listar() {
        return usuarios.findAll();
    }

    public Usuario criar(String login, String senha, String nome,
                         String perfil, boolean deveTrocarSenha) {
        Seguranca.validarPerfil(perfil);
        if (login == null || login.isBlank())
            throw new RequisicaoInvalidaException("Login obrigatorio.");
        if (usuarios.existsByLogin(login))
            throw new RequisicaoInvalidaException(
                    "O login '" + login + "' ja existe.");
        if (senha == null || senha.length() < 4)
            throw new RequisicaoInvalidaException(
                    "A senha deve ter pelo menos 4 caracteres.");
        Usuario usuario = new Usuario(login.trim(),
                codificador.encode(senha), nome, perfil);
        usuario.setDeveTrocarSenha(deveTrocarSenha);
        return usuarios.save(usuario);
    }

    public Usuario atualizar(Long id, String nome, String senha,
                             String perfil, boolean deveTrocarSenha) {
        Seguranca.validarPerfil(perfil);
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario " + id + " nao encontrado."));
        if (Seguranca.PERFIL_ADMIN.equals(usuario.getPerfil())
                && !Seguranca.PERFIL_ADMIN.equals(perfil)
                && contarAdmins() <= 1) {
            throw new RequisicaoInvalidaException(
                    "Nao e possivel remover o perfil do ultimo administrador.");
        }
        usuario.setNome(nome);
        usuario.setPerfil(perfil);
        usuario.setDeveTrocarSenha(deveTrocarSenha);
        if (senha != null && !senha.isBlank()) {
            if (senha.length() < 4)
                throw new RequisicaoInvalidaException(
                        "A senha deve ter pelo menos 4 caracteres.");
            usuario.setSenhaHash(codificador.encode(senha));
        }
        return usuarios.save(usuario);
    }

    /** Confere a senha atual do usuario (usada na liberacao do Swagger). */
    public boolean confirmarSenha(Usuario usuario, String senha) {
        return senha != null && codificador.matches(senha, usuario.getSenhaHash());
    }

    /** Confere a senha temporaria e troca pela senha escolhida pelo usuario. */
    public Usuario trocarSenha(Usuario usuario, String senhaAtual,
                               String novaSenha) {
        if (senhaAtual == null
                || !codificador.matches(senhaAtual, usuario.getSenhaHash()))
            throw new RequisicaoInvalidaException("Senha atual invalida.");
        if (novaSenha == null || novaSenha.length() < 4)
            throw new RequisicaoInvalidaException(
                    "A nova senha deve ter pelo menos 4 caracteres.");
        usuario.setSenhaHash(codificador.encode(novaSenha));
        usuario.setDeveTrocarSenha(false);
        return usuarios.save(usuario);
    }

    public void remover(Long id, Usuario quemPede) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario " + id + " nao encontrado."));
        if (usuario.getLogin().equals(quemPede.getLogin()))
            throw new RequisicaoInvalidaException(
                    "Voce nao pode excluir o proprio usuario logado.");
        if (Seguranca.PERFIL_ADMIN.equals(usuario.getPerfil())
                && contarAdmins() <= 1) {
            throw new RequisicaoInvalidaException(
                    "Nao e possivel excluir o ultimo administrador.");
        }
        usuarios.delete(usuario);
    }

    private long contarAdmins() {
        return usuarios.findAll().stream()
                .filter(u -> Seguranca.PERFIL_ADMIN.equals(u.getPerfil()))
                .count();
    }
}
