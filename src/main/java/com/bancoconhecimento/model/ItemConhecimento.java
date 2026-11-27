package com.bancoconhecimento.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Item do banco de conhecimento (registro com titulo, categoria, descricao
 *  e anexos de foto/video). */
@Entity
@Table(name = "itens_conhecimento")
public class ItemConhecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 80)
    private String categoria;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, length = 60)
    private String autor;

    /** true = visivel somente para o perfil ADMIN (direcao). */
    @Column
    private Boolean somenteAdmin;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<Anexo> anexos = new ArrayList<>();

    public ItemConhecimento() {
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public Boolean getSomenteAdmin() { return somenteAdmin; }
    public void setSomenteAdmin(Boolean somenteAdmin) {
        this.somenteAdmin = somenteAdmin;
    }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public List<Anexo> getAnexos() { return anexos; }

    public void adicionarAnexo(Anexo anexo) {
        anexo.setItem(this);
        this.anexos.add(anexo);
    }
}
