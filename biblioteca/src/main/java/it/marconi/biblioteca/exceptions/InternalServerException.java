package it.marconi.biblioteca.exceptions;

/**
 * Lanciata per errori interni del server (HTTP 500)
 * NON espone dettagli tecnici al client
 */
public class InternalServerException extends APIException {
    public InternalServerException(String message) {
        super("INTERNAL_ERROR", message);
    }
}
