package com.tuti.daos.informes.exception;

public class ConflictoOperacionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public ConflictoOperacionException(String message) {
        super(message);
    }
}
