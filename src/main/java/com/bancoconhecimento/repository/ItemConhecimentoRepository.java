package com.bancoconhecimento.repository;

import com.bancoconhecimento.model.ItemConhecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemConhecimentoRepository
        extends JpaRepository<ItemConhecimento, Long> {

    List<ItemConhecimento> findByCategoria(String categoria);

    @Query("""
            SELECT i FROM ItemConhecimento i
            WHERE (:query IS NULL OR LOWER(i.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(i.descricao) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:categoria IS NULL OR i.categoria = :categoria)
            ORDER BY i.criadoEm DESC
            """)
    List<ItemConhecimento> buscar(@Param("query") String query,
                                  @Param("categoria") String categoria);

    @Query("SELECT DISTINCT i.categoria FROM ItemConhecimento i "
            + "WHERE i.categoria IS NOT NULL ORDER BY i.categoria")
    List<String> categoriasDistintas();
}
