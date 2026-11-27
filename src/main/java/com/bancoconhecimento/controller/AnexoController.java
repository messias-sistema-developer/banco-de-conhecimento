package com.bancoconhecimento.controller;

import com.bancoconhecimento.model.Anexo;
import com.bancoconhecimento.service.ConhecimentoService;
import com.bancoconhecimento.service.Seguranca;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Streaming dos anexos guardados no banco. O suporte a Range deixa o
 *  player de video avancar/retroceder sem baixar tudo de novo. */
@RestController
@RequestMapping("/api/anexos")
@Tag(name = "Anexos", description = "Stream de fotos, videos e arquivos")
public class AnexoController {

    private final ConhecimentoService servico;

    public AnexoController(ConhecimentoService servico) {
        this.servico = servico;
    }

    @GetMapping("/{id}/stream")
    @Operation(summary = "Baixar/transmitir o conteudo do anexo "
            + "(suporta Range para video)")
    public ResponseEntity<ByteArrayResource> stream(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String range) {

        Anexo anexo = servico.obterAnexo(id);
        byte[] dados = anexo.getDados();
        MediaType tipo = MediaType.parseMediaType(anexo.getContentType());

        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(tipo);
        cabecalhos.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + anexo.getNome().replace("\"", "") + "\"");
        cabecalhos.setCacheControl("public, max-age=3600");

        if (range != null && range.startsWith("bytes=")) {
            String[] partes = range.substring(6).split("-", 2);
            long inicio = Long.parseLong(partes[0].trim());
            long fim = partes.length > 1 && !partes[1].isBlank()
                    ? Long.parseLong(partes[1].trim())
                    : dados.length - 1;
            fim = Math.min(fim, dados.length - 1);
            int tamanho = (int) (fim - inicio + 1);
            byte[] pedaco = new byte[tamanho];
            System.arraycopy(dados, (int) inicio, pedaco, 0, tamanho);
            cabecalhos.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + inicio + "-" + fim + "/" + dados.length);
            cabecalhos.setContentLength(tamanho);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(cabecalhos).body(new ByteArrayResource(pedaco) {
                        @Override public String getFilename() {
                            return anexo.getNome();
                        }
                    });
        }

        cabecalhos.setContentLength(dados.length);
        cabecalhos.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        return ResponseEntity.ok().headers(cabecalhos)
                .body(new ByteArrayResource(dados) {
                    @Override public String getFilename() {
                        return anexo.getNome();
                    }
                });
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir um anexo")
    public java.util.Map<String, Object> remover(@PathVariable Long id,
                                                  HttpServletRequest request) {
        // quem pode incluir (ADMIN e USER1) pode remover o anexo errado
        // para substituir pelo correto; excluir o REGISTRO inteiro segue
        // restrito ao ADMIN
        Seguranca.exigir(request, Seguranca.PERFIL_ADMIN,
                Seguranca.PERFIL_USER1);
        servico.removerAnexo(id);
        return java.util.Map.of("ok", true, "id", id);
    }
}
