package com.bancoconhecimento.service;

import com.bancoconhecimento.config.AutorizacaoInterceptor;
import com.bancoconhecimento.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;

/** Perfis e checagem de permissao por perfil (no servidor). */
public final class Seguranca {

    public static final String PERFIL_ADMIN = "ADMIN";
    public static final String PERFIL_USER1 = "USER1";
    public static final String PERFIL_USER2 = "USER2";

    private Seguranca() {
    }

    /** Nome amigavel do perfil para a tela. */
    public static String rotulo(String perfil) {
        return switch (perfil == null ? "" : perfil) {
            case PERFIL_ADMIN -> "Direção (administrador)";
            case PERFIL_USER1 -> "Administrativo";
            case PERFIL_USER2 -> "Operacional";
            default -> perfil;
        };
    }

    /** So aceita os tres perfis validos. */
    public static void validarPerfil(String perfil) {
        if (!PERFIL_ADMIN.equals(perfil) && !PERFIL_USER1.equals(perfil)
                && !PERFIL_USER2.equals(perfil)) {
            throw new IllegalArgumentException(
                    "Perfil invalido. Use ADMIN, USER1 ou USER2.");
        }
    }

    /** Lanca PermissaoNegadaException se o usuario da sessao nao tiver um
     *  dos perfis informados. */
    public static void exigir(HttpServletRequest request, String... perfis) {
        Usuario usuario = (Usuario) request.getSession()
                .getAttribute(AutorizacaoInterceptor.USUARIO_SESSAO);
        String perfil = usuario != null ? usuario.getPerfil() : null;
        for (String p : perfis) {
            if (p.equals(perfil)) return;
        }
        throw new PermissaoNegadaException(perfil, perfis);
    }

    /** Erro 403: acao nao permitida para o perfil atual. */
    public static class PermissaoNegadaException extends RuntimeException {

        public PermissaoNegadaException(String perfilAtual, String[] exigidos) {
            super(String.format(
                    "Seu perfil (%s) nao permite esta acao. Perfis autorizados: %s.",
                    perfilAtual == null ? "sem perfil" : perfilAtual,
                    String.join(", ", exigidos)));
        }
    }
}
