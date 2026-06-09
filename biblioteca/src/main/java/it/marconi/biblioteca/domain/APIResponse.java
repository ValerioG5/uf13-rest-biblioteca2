package it.marconi.biblioteca.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Contratto standardizzato per tutte le risposte API
 * Ogni risposta contiene: success, code, message, data, timestamp
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record APIResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    LocalDateTime timestamp
) {

    // Risposta di successo con dati
    public static <T> APIResponse<T> ok(T data, String message) {
        return new APIResponse<>(
            true,
            "SUCCESS",
            message,
            data,
            LocalDateTime.now()
        );
    }

    // Risposta di successo con dati e messaggio di default
    public static <T> APIResponse<T> ok(T data) {
        return ok(data, "Operazione completata con successo");
    }

    // Risposta di successo senza dati
    public static <T> APIResponse<T> success(String message) {
        return new APIResponse<>(
            true,
            "SUCCESS",
            message,
            null,
            LocalDateTime.now()
        );
    }

    // Errore generico
    public static <T> APIResponse<T> error(String code, String message, T data) {
        return new APIResponse<>(
            false,
            code,
            message,
            data,
            LocalDateTime.now()
        );
    }

    public static <T> APIResponse<T> error(String code, String message) {
        return new APIResponse<>(
                false,
                code,
                message,
                null,
                LocalDateTime.now()
        );
    }

    // Errore con fallback
    public static <T> APIResponse<T> error(String message) {
        return error("ERROR", message, null);
    }
    
}
