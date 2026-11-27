package com.bancoconhecimento.config;

import com.bancoconhecimento.model.Usuario;
import com.bancoconhecimento.service.Seguranca;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutorizacaoInterceptor autorizacao;

    public WebConfig(AutorizacaoInterceptor autorizacao) {
        this.autorizacao = autorizacao;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autorizacao)
                .addPathPatterns("/api/**", "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs*.yaml",
                        "/swagger-gate.html",
                        "/h2-console", "/h2-console/**")
                .excludePathPatterns("/api/auth/login");
    }

    /** O console do H2 e um servlet, entao nao passa pelo interceptor. */
    @Bean
    public FilterRegistrationBean<Filter> protecaoConsoleH2() {
        FilterRegistrationBean<Filter> registro = new FilterRegistrationBean<>();
        registro.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res,
                                 FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) req;
                HttpSession sessao = request.getSession(false);
                Object atributo = sessao == null ? null
                        : sessao.getAttribute(AutorizacaoInterceptor.USUARIO_SESSAO);
                boolean admin = atributo instanceof Usuario u
                        && Seguranca.PERFIL_ADMIN.equals(u.getPerfil());
                if (!admin) {
                    ((HttpServletResponse) res).sendRedirect("/login.html");
                    return;
                }
                chain.doFilter(req, res);
            }
        });
        registro.addUrlPatterns("/h2-console/*");
        return registro;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // raiz cai direto na tela de login
        registry.addRedirectViewController("/", "/login.html");
    }
}
