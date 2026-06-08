package it.marconi.biblioteca.exceptions;

/**
 * Eccezione di base per errori API controllati
 * Ogni eccezione contiene un code e un messaggio per il client
 */
public abstract class APIException extends RuntimeException {
    private final String code;

    public APIException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
