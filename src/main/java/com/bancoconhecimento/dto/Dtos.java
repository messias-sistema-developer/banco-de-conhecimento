package com.bancoconhecimento.dto;

import com.bancoconhecimento.model.Anexo;
import com.bancoconhecimento.model.ItemConhecimento;
import jakarta.validation.constraints.NotBlank;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** DTOs do projeto (login, itens e anexos). */
public final class Dtos {

    private Dtos() {
    }

    // ------------------------------------------------------------- auth

    public record LoginRequest(@NotBlank String login,
                               @NotBlank String senha) {
    }

    public record UsuarioResponse(String login, String nome, String perfil,
                                  boolean deveTrocarSenha) {
    }

    public record UsuarioItem(Long id, String login, String nome,
                              String perfil, boolean deveTrocarSenha) {
    }

    public record CriarUsuarioRequest(@NotBlank String login,
                                      @NotBlank String senha,
                                      @NotBlank String nome,
                                      @NotBlank String perfil,
                                      boolean deveTrocarSenha) {
    }

    public record AtualizarUsuarioRequest(@NotBlank String nome,
                                          String senha,
                                          @NotBlank String perfil,
                                          boolean deveTrocarSenha) {
    }

    public record TrocarSenhaRequest(@NotBlank String senhaAtual,
                                     @NotBlank String novaSenha) {
    }

    public record SenhaAdminRequest(@NotBlank String senha) {
    }

    // ------------------------------------------------------------- item

    public record ItemRequest(@NotBlank String titulo,
                              String categoria,
                              String descricao,
                              Boolean somenteAdmin) {
    }

    public record AnexoResponse(Long id, String nome, String contentType,
                                long tamanhoBytes, String tipo) {

        public static AnexoResponse de(Anexo a) {
            return new AnexoResponse(a.getId(), a.getNome(),
                    a.getContentType(), a.getTamanhoBytes(), a.getTipo());
        }
    }

    public record ItemResponse(Long id, String titulo, String categoria,
                               String descricao, String autor, String criadoEm,
                               Boolean somenteAdmin,
                               List<AnexoResponse> anexos) {

        private static final DateTimeFormatter FMT =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        public static ItemResponse de(ItemConhecimento i) {
            List<AnexoResponse> anexos = new ArrayList<>();
            if (i.getAnexos() != null) {
                i.getAnexos().forEach(a -> anexos.add(AnexoResponse.de(a)));
            }
            return new ItemResponse(i.getId(), i.getTitulo(), i.getCategoria(),
                    i.getDescricao(), i.getAutor(),
                    i.getCriadoEm() != null ? i.getCriadoEm().format(FMT) : "",
                    Boolean.TRUE.equals(i.getSomenteAdmin()), anexos);
        }
    }
}
