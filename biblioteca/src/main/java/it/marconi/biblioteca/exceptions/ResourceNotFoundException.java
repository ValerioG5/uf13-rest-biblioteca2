package it.marconi.biblioteca.exceptions;

/**
 * Lanciata quando una risorsa non è trovata (HTTP 404)
 */
public class ResourceNotFoundException extends APIException {
    public ResourceNotFoundException(String resourceName, Integer id) {
        super("RESOURCE_NOT_FOUND", resourceName + " con ID " + id + " non trovato");
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
