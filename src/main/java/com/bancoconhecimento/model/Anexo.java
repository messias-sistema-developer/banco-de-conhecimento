package com.bancoconhecimento.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Foto, video ou arquivo armazenado DENTRO do banco (coluna BLOB). */
@Entity
@Table(name = "anexos")
public class Anexo {

    public static final String TIPO_FOTO = "FOTO";
    public static final String TIPO_VIDEO = "VIDEO";
    public static final String TIPO_ARQUIVO = "ARQUIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long tamanhoBytes;

    @Column(nullable = false, length = 10)
    private String tipo;

    /** Conteudo binario (foto/video/arquivo) guardado no SQL. */
    @Lob
    @Column(nullable = false, columnDefinition = "BLOB")
    private byte[] dados;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private ItemConhecimento item;

    public Anexo() {
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }
    public ItemConhecimento getItem() { return item; }
    public void setItem(ItemConhecimento item) { this.item = item; }
}
