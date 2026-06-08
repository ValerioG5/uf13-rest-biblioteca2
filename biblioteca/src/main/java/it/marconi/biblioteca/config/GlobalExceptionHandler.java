package it.marconi.biblioteca.config;

import java.util.stream.Collectors;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import it.marconi.biblioteca.domain.APIResponse;
import it.marconi.biblioteca.exceptions.APIException;
import it.marconi.biblioteca.exceptions.InternalServerException;
import it.marconi.biblioteca.exceptions.InvalidRequestException;
import it.marconi.biblioteca.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestisce TUTTI gli errori dell'applicazione in modo centralizzato
 * @RestControllerAdvice intercetta le eccezioni da tutti i controller
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gestisce eccezioni custom di tipo APIException
     * Queste sono errori "previsti" e controllati
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse<?>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("ResourceNotFoundException: {}", ex.getMessage());

        APIResponse<?> response = APIResponse.error(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<APIResponse<?>> handleInvalidRequest(
            InvalidRequestException ex, WebRequest request) {

        log.warn("InvalidRequestException: {}", ex.getMessage());

        APIResponse<?> response = APIResponse.error(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<APIResponse<?>> handleInternalServer(
            InternalServerException ex, WebRequest request) {

        log.error("InternalServerException: {}", ex.getMessage());

        APIResponse<?> response = APIResponse.error(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Gestisce errori di validazione (quando @Valid fallisce)
     * Raccoglie TUTTI i messaggi di validazione in un'unica risposta
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<?>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                e -> e.getField(),
                e -> e.getDefaultMessage(),
                (existing, replacement) -> existing
            ));

        log.warn("Validation failed: {}", errors);

        APIResponse<?> response = APIResponse.error(
            "VALIDATION_ERROR",
            "Validazione fallita",
            errors
        );

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
}

    /**
     * Gestisce errori di type mismatch nei parametri (es. stringa invece di numero)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<?>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Type mismatch: {} - Expected {} but got {}",
            ex.getName(), ex.getRequiredType().getSimpleName(), ex.getValue());

        String message = "Il parametro '" + ex.getName() + "' deve essere di tipo " +
                        ex.getRequiredType().getSimpleName();
        APIResponse<?> response = APIResponse.error("INVALID_PARAMETER", message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Gestisce endpoint non trovati (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<APIResponse<?>> handleNotFound(
            NoHandlerFoundException ex, WebRequest request) {

        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        APIResponse<?> response = APIResponse.error("NOT_FOUND",
            "Endpoint non trovato: " + ex.getRequestURL());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Catch-all: gestisce QUALSIASI eccezione non prevista
     * Non espone lo stack trace al client
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<?>> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected exception occurred", ex);

        // Messaggio generico per il client (NO stack trace!)
        APIResponse<?> response = APIResponse.error("INTERNAL_ERROR",
            "Si è verificato un errore interno. Contattare il supporto.");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
