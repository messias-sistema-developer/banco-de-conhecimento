package com.bancoconhecimento.service;

import com.bancoconhecimento.model.Anexo;
import com.bancoconhecimento.model.ItemConhecimento;
import com.bancoconhecimento.repository.AnexoRepository;
import com.bancoconhecimento.repository.ItemConhecimentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/** Regras de negocio: CRUD de itens e upload de anexos com limites. */
@Service
public class ConhecimentoService {

    private final ItemConhecimentoRepository itens;
    private final AnexoRepository anexos;

    @Value("${app.upload.foto-max-mb:5}")
    private long fotoMaxMb;

    @Value("${app.upload.video-max-mb:50}")
    private long videoMaxMb;

    @Value("${app.upload.arquivo-max-mb:10}")
    private long arquivoMaxMb;

    public ConhecimentoService(ItemConhecimentoRepository itens,
                               AnexoRepository anexos) {
        this.itens = itens;
        this.anexos = anexos;
    }

    // ------------------------------------------------------------ itens

    @Transactional(readOnly = true)
    public List<ItemConhecimento> buscar(String query, String categoria,
                                         boolean visaoAdmin) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        String c = (categoria == null || categoria.isBlank()
                || "todas".equals(categoria)) ? null : categoria.trim();
        List<ItemConhecimento> lista = itens.buscar(q, c);
        if (visaoAdmin) return lista;
        // registros marcados como restritos: so o ADMIN visualiza
        return lista.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getSomenteAdmin()))
                .toList();
    }

    public ItemConhecimento criar(String titulo, String categoria,
                                  String descricao, String autor,
                                  Boolean somenteAdmin, boolean isAdmin) {
        ItemConhecimento item = new ItemConhecimento();
        item.setTitulo(titulo);
        item.setCategoria(categoria);
        item.setDescricao(descricao);
        item.setAutor(autor);
        item.setCriadoEm(LocalDateTime.now());
        // so o ADMIN pode restringir a visibilidade do registro
        item.setSomenteAdmin(isAdmin && Boolean.TRUE.equals(somenteAdmin));
        return itens.save(item);
    }

    public ItemConhecimento atualizar(Long id, String titulo, String categoria,
                                      String descricao, Boolean somenteAdmin,
                                      boolean isAdmin) {
        ItemConhecimento item = itens.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item " + id + " nao encontrado."));
        item.setTitulo(titulo);
        item.setCategoria(categoria);
        item.setDescricao(descricao);
        if (isAdmin && somenteAdmin != null) {
            item.setSomenteAdmin(somenteAdmin);
        }
        return itens.save(item);
    }

    @Transactional
    public void remover(Long id) {
        itens.findById(id).ifPresent(itens::delete);
    }

    // --------------------------------------------------------- uploads

    /** Guarda a foto/video/arquivo DENTRO do banco, respeitando o limite
     *  do tipo (LimiteExcedidoException quando passar do permitido). */
    @Transactional
    public Anexo anexar(Long itemId, MultipartFile arquivo) throws IOException {
        ItemConhecimento item = itens.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item " + itemId + " nao encontrado."));

        String contentType = arquivo.getContentType() == null
                ? "application/octet-stream" : arquivo.getContentType();
        String tipo = classificar(contentType);

        long limite = switch (tipo) {
            case Anexo.TIPO_FOTO -> fotoMaxMb;
            case Anexo.TIPO_VIDEO -> videoMaxMb;
            default -> arquivoMaxMb;
        };
        long bytes = arquivo.getSize();
        if (bytes > limite * 1024L * 1024L) {
            throw new LimiteExcedidoException(tipo, bytes, limite);
        }

        Anexo anexo = new Anexo();
        anexo.setNome(arquivo.getOriginalFilename() == null
                ? "arquivo" : arquivo.getOriginalFilename());
        anexo.setContentType(contentType);
        anexo.setTamanhoBytes(bytes);
        anexo.setTipo(tipo);
        anexo.setDados(arquivo.getBytes());
        item.adicionarAnexo(anexo);
        return anexos.save(anexo);
    }

    @Transactional
    public void removerAnexo(Long anexoId) {
        anexos.findById(anexoId).ifPresent(anexos::delete);
    }

    public Anexo obterAnexo(Long id) {
        return anexos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Anexo " + id + " nao encontrado."));
    }

    /** Contagem de registros por periodo (DIA = ultimos 30 dias,
     *  MES = ultimos 12 meses, ANO = todos os anos). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> estatisticas(String granularidade) {
        String g = granularidade == null ? "MES"
                : granularidade.toUpperCase();
        List<ItemConhecimento> todos = itens.findAll();
        java.time.LocalDateTime agora = LocalDateTime.now();
        java.util.LinkedHashMap<String, Long> contagem =
                new java.util.LinkedHashMap<>();

        switch (g) {
            case "DIA" -> {
                java.time.format.DateTimeFormatter fmt =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM");
                java.time.LocalDate inicio = agora.toLocalDate().minusDays(29);
                for (java.time.LocalDate d = inicio;
                        !d.isAfter(agora.toLocalDate()); d = d.plusDays(1)) {
                    contagem.put(d.format(fmt), 0L);
                }
                for (ItemConhecimento i : todos) {
                    if (i.getCriadoEm() == null) continue;
                    java.time.LocalDate d = i.getCriadoEm().toLocalDate();
                    if (!d.isBefore(inicio)) {
                        contagem.merge(d.format(fmt), 1L, Long::sum);
                    }
                }
            }
            case "ANO" -> {
                for (ItemConhecimento i : todos) {
                    if (i.getCriadoEm() == null) continue;
                    contagem.merge(String.valueOf(
                            i.getCriadoEm().getYear()), 1L, Long::sum);
                }
                java.util.LinkedHashMap<String, Long> ordenado =
                        new java.util.LinkedHashMap<>();
                contagem.entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .forEach(e -> ordenado.put(e.getKey(), e.getValue()));
                contagem = ordenado;
            }
            default -> { // MES: ultimos 12 meses, incluindo meses sem registro
                java.time.format.DateTimeFormatter fmt =
                        java.time.format.DateTimeFormatter.ofPattern("MM/yyyy");
                java.time.YearMonth inicio = java.time.YearMonth
                        .from(agora).minusMonths(11);
                for (java.time.YearMonth m = inicio;
                        !m.isAfter(java.time.YearMonth.from(agora));
                        m = m.plusMonths(1)) {
                    contagem.put(m.format(fmt), 0L);
                }
                for (ItemConhecimento i : todos) {
                    if (i.getCriadoEm() == null) continue;
                    java.time.YearMonth m =
                            java.time.YearMonth.from(i.getCriadoEm());
                    if (!m.isBefore(inicio)) {
                        contagem.merge(m.format(fmt), 1L, Long::sum);
                    }
                }
            }
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (var e : contagem.entrySet()) {
            Map<String, Object> linha = new java.util.HashMap<>();
            linha.put("rotulo", e.getKey());
            linha.put("contagem", e.getValue());
            resultado.add(linha);
        }
        return resultado;
    }

    public List<String> categorias() {
        return itens.categoriasDistintas();
    }

    private String classificar(String contentType) {
        if (contentType.startsWith("image/")) return Anexo.TIPO_FOTO;
        if (contentType.startsWith("video/")) return Anexo.TIPO_VIDEO;
        return Anexo.TIPO_ARQUIVO;
    }

    /** Erro de negocio: arquivo acima do limite permitido. */
    public static class LimiteExcedidoException extends RuntimeException {

        public final String tipo;
        public final long bytes;
        public final long limiteMb;

        public LimiteExcedidoException(String tipo, long bytes, long limiteMb) {
            super(String.format(
                    "Limite excedido: o %s tem %s e o maximo permitido e %d MB.",
                    tipo.toLowerCase(), formatarBytes(bytes), limiteMb));
            this.tipo = tipo;
            this.bytes = bytes;
            this.limiteMb = limiteMb;
        }

        private static String formatarBytes(long b) {
            if (b >= 1024L * 1024L) return String.format("%.1f MB", b / 1048576.0);
            return b / 1024 + " KB";
        }
    }
}
