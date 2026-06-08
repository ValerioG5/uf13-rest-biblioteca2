package it.marconi.biblioteca.exceptions;

/**
 * Lanciata quando i dati forniti sono invalidi (HTTP 400)
 */
public class InvalidRequestException extends APIException {
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }
}
