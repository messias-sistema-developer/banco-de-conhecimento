package com.bancoconhecimento.controller;

import com.bancoconhecimento.config.AutorizacaoInterceptor;
import com.bancoconhecimento.dto.Dtos.AnexoResponse;
import com.bancoconhecimento.dto.Dtos.ItemRequest;
import com.bancoconhecimento.dto.Dtos.ItemResponse;
import com.bancoconhecimento.model.ItemConhecimento;
import com.bancoconhecimento.model.Usuario;
import com.bancoconhecimento.service.ConhecimentoService;
import com.bancoconhecimento.service.Seguranca;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Map;

@RestController
@RequestMapping("/api/itens")
@Tag(name = "Itens de Conhecimento",
        description = "Cadastro, busca e exclusao dos registros")
public class ConhecimentoController {

    private final ConhecimentoService servico;

    public ConhecimentoController(ConhecimentoService servico) {
        this.servico = servico;
    }

    private Usuario usuarioAtual(HttpServletRequest request) {
        return (Usuario) request.getSession()
                .getAttribute(AutorizacaoInterceptor.USUARIO_SESSAO);
    }

    private Usuario perfilAtual(HttpServletRequest request) {
        return usuarioAtual(request);
    }

    @GetMapping
    @Operation(summary = "Listar/buscar itens (query e categoria opcionais)")
    public List<ItemResponse> listar(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String categoria,
            HttpServletRequest request) {
        Usuario usuario = usuarioAtual(request);
        boolean visaoAdmin = usuario != null && Seguranca.PERFIL_ADMIN
                .equals(usuario.getPerfil());
        return servico.buscar(query, categoria, visaoAdmin).stream()
                .map(ItemResponse::de).toList();
    }

    @GetMapping("/estatisticas/registros")
    @Operation(summary = "Dashboard do ADMIN: registros por periodo. "
            + "granularidade = DIA (ultimos 30 dias), MES (ultimos 12 meses) "
            + "ou ANO (todos)")
    public List<Map<String, Object>> estatisticas(
            @RequestParam(defaultValue = "MES") String granularidade,
            HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN);
        return servico.estatisticas(granularidade);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorias ja cadastradas")
    public List<String> categorias() {
        return servico.categorias();
    }

    @PostMapping
    @Operation(summary = "Criar um novo item")
    public ItemResponse criar(@Valid @RequestBody ItemRequest corpo,
                              HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN, Seguranca.PERFIL_USER1);
        Usuario autor = usuarioAtual(request);
        boolean isAdmin = Seguranca.PERFIL_ADMIN.equals(
                autor != null ? autor.getPerfil() : "");
        ItemConhecimento item = servico.criar(corpo.titulo(),
                corpo.categoria(), corpo.descricao(),
                autor != null ? autor.getNome() : "sistema",
                corpo.somenteAdmin(), isAdmin);
        return ItemResponse.de(item);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar titulo/categoria/descricao")
    public ItemResponse atualizar(@PathVariable Long id,
                                  @Valid @RequestBody ItemRequest corpo,
                                  HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN, Seguranca.PERFIL_USER1);
        boolean isAdmin = Seguranca.PERFIL_ADMIN.equals(
                perfilAtual(request).getPerfil());
        return ItemResponse.de(servico.atualizar(id, corpo.titulo(),
                corpo.categoria(), corpo.descricao(),
                corpo.somenteAdmin(), isAdmin));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir item e seus anexos")
    public Map<String, Object> remover(@PathVariable Long id,
                                       HttpServletRequest request) {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN);
        servico.remover(id);
        return Map.of("ok", true, "id", id);
    }

    @PostMapping("/{id}/anexos")
    @Operation(summary = "Anexar foto, video ou arquivo (limites: "
            + "foto 5 MB, video 50 MB, arquivo 10 MB)")
    public AnexoResponse anexar(@PathVariable Long id,
                                @RequestParam("arquivo")
                                org.springframework.web.multipart.MultipartFile arquivo,
                                HttpServletRequest request)
            throws Exception {
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN, Seguranca.PERFIL_USER1);
        return AnexoResponse.de(servico.anexar(id, arquivo));
    }
}
