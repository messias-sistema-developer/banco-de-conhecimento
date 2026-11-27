package com.bancoconhecimento.exception;

/** Erro 400: regra de negocio/validacao nao atendida. */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
