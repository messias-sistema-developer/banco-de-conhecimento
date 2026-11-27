package com.bancoconhecimento.exception;

import com.bancoconhecimento.exception.RequisicaoInvalidaException;
import com.bancoconhecimento.service.ConhecimentoService.LimiteExcedidoException;
import com.bancoconhecimento.service.Seguranca.PermissaoNegadaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<Map<String, Object>> limite(LimiteExcedidoException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("erro", e.getMessage(),
                        "tipo", e.tipo,
                        "limiteMb", e.limiteMb,
                        "bytesEnviados", e.bytes));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> multipart(
            MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("erro",
                        "Arquivo acima do limite global de upload (200 MB)."));
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> requisicaoInvalida(
            RequisicaoInvalidaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(PermissaoNegadaException.class)
    public ResponseEntity<Map<String, Object>> semPermissao(
            PermissaoNegadaException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(
            IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generico(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("erro", "Erro interno: " + e.getMessage()));
    }
}
