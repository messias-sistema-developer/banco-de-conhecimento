package com.bancoconhecimento.config;

import com.bancoconhecimento.model.Usuario;
import com.bancoconhecimento.service.Seguranca;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Bloqueia as chamadas de API para quem nao fez login. Swagger so e liberado
 *  apos o ADMIN confirmar a senha na sessao; o console do H2 exige ADMIN. */
@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    public static final String USUARIO_SESSAO = "USUARIO_LOGADO";
    public static final String SWAGGER_LIBERADO = "SWAGGER_LIBERADO";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String caminho = request.getRequestURI();

        boolean swagger = caminho.equals("/swagger-ui.html")
                || caminho.startsWith("/swagger-ui/")
                || caminho.equals("/v3/api-docs")
                || caminho.startsWith("/v3/api-docs/")
                || (caminho.startsWith("/v3/api-docs") && caminho.endsWith(".yaml"));
        boolean gate = caminho.equals("/swagger-gate.html");
        boolean consoleH2 = caminho.equals("/h2-console")
                || caminho.startsWith("/h2-console/");

        // Swagger: qualquer acesso vai para a tela da senha ate ser liberado
        if (swagger) {
            HttpSession sessaoSwagger = request.getSession(false);
            boolean liberado = sessaoSwagger != null
                    && Boolean.TRUE.equals(sessaoSwagger.getAttribute(SWAGGER_LIBERADO));
            if (!liberado) {
                response.sendRedirect("/swagger-gate.html");
                return false;
            }
            return true;
        }

        // A tela da senha pode ser aberta diretamente; a API de liberacao
        // exige ADMIN logado e devolve 401 se a sessao tiver expirado.
        if (gate) {
            return true;
        }

        // Console do H2: somente ADMIN logado
        if (consoleH2) {
            HttpSession sessaoAdmin = request.getSession(false);
            Object atributoAdmin = sessaoAdmin == null ? null
                    : sessaoAdmin.getAttribute(USUARIO_SESSAO);
            if (!(atributoAdmin instanceof Usuario usuario)
                    || !Seguranca.PERFIL_ADMIN.equals(usuario.getPerfil())) {
                response.sendRedirect("/login.html");
                return false;
            }
            return true;
        }

        HttpSession sessao = request.getSession(false);
        Object atributo = sessao == null ? null
                : sessao.getAttribute(USUARIO_SESSAO);
        if (atributo instanceof Usuario usuario) {
            if (caminho.startsWith("/api/")) {
                // impede o navegador de guardar respostas antigas
                // (ex: lista de categorias desatualizada no filtro)
                response.setHeader("Cache-Control", "no-store");
                if (usuario.isDeveTrocarSenha()
                        && !caminho.equals("/api/auth/me")
                        && !caminho.equals("/api/auth/trocar-senha")
                        && !caminho.equals("/api/auth/logout")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"erro\":\"Troque a senha temporaria para continuar.\"}");
                    return false;
                }
            }
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"erro\":\"Sessao expirada. Faca login novamente.\"}");
        return false;
    }
}
