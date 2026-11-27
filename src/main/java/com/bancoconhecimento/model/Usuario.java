package com.bancoconhecimento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuario de acesso ao banco de conhecimento. */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String login;

    @Column(nullable = false)
    private String senhaHash;

    @Column(nullable = false, length = 100)
    private String nome;

    /** PERFIL_ADMIN (direcao), PERFIL_USER1 (administrativo) ou
     *  PERFIL_USER2 (operacional). */
    @Column(nullable = false, length = 10)
    private String perfil;

    /** Senha definida pelo ADMIN que obrigatoriamente deve ser trocada. */
    @Column(name = "deve_trocar_senha")
    private Boolean deveTrocarSenha;

    public Usuario() {
    }

    public Usuario(String login, String senhaHash, String nome, String perfil) {
        this.login = login;
        this.senhaHash = senhaHash;
        this.nome = nome;
        this.perfil = perfil;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getSenhaHash() { return senhaHash; }
    public String getNome() { return nome; }
    public String getPerfil() { return perfil; }
    public boolean isDeveTrocarSenha() {
        return Boolean.TRUE.equals(deveTrocarSenha);
    }

    public void setNome(String nome) { this.nome = nome; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public void setDeveTrocarSenha(boolean deveTrocarSenha) {
        this.deveTrocarSenha = deveTrocarSenha;
    }
}
