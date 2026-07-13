package it.marconi.biblioteca.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.exceptions.ResourceNotFoundException;
import it.marconi.biblioteca.services.AutoreService;
import it.marconi.biblioteca.services.LibroService;

/**
 * Test di integrazione focalizzati sul Web Layer.
 * Usa @WebMvcTest per caricare solo il controller e il GlobalExceptionHandler,
 * senza avviare l'intero contesto Spring (no DB, no service reali).
 */
@WebMvcTest(AutoreController.class)
@DisplayName("AutoreController – Integration Tests (Web Layer)")
class AutoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // MockitoBean sostituisce il bean nel contesto Spring con un mock Mockito
    @MockitoBean
    private AutoreService autoreService;

    @MockitoBean
    private LibroService libroService;

    // ------------------------------------------------------------------ //
    //  GET /autori  – lista di tutti gli autori                           //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /autori – 200 OK: restituisce lista autori con struttura corretta")
    void getAll_shouldReturn200WithAutoriList() throws Exception {

        AutoreDTO autore1 = new AutoreDTO(1, "Dante", "Alighieri");
        AutoreDTO autore2 = new AutoreDTO(2, "Alessandro", "Manzoni");

        when(autoreService.findAll()).thenReturn(List.of(autore1, autore2));

        mockMvc.perform(get("/autori"))
                .andExpect(status().isOk())
                // struttura envelope
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Lista autori recuperata con successo"))
                // payload
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].nome").value("Dante"))
                .andExpect(jsonPath("$.data[0].cognome").value("Alighieri"));
    }

    // ------------------------------------------------------------------ //
    //  GET /autori/{id} – autore esistente                                //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /autori/{id} – 200 OK: autore trovato con dati corretti")
    void getAutore_existingId_shouldReturn200WithAutore() throws Exception {

        AutoreDTO autore = new AutoreDTO(1, "Dante", "Alighieri");
        when(autoreService.getById(1)).thenReturn(autore);

        mockMvc.perform(get("/autori/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Autore trovato"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nome").value("Dante"))
                .andExpect(jsonPath("$.data.cognome").value("Alighieri"));
    }

    // ------------------------------------------------------------------ //
    //  GET /autori/{id} – autore non trovato                              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /autori/{id} – 404 Not Found: autore inesistente restituisce errore strutturato")
    void getAutore_notFound_shouldReturn404WithErrorEnvelope() throws Exception {

        when(autoreService.getById(999))
                .thenThrow(new ResourceNotFoundException("Autore", 999));

        mockMvc.perform(get("/autori/999"))
                .andExpect(status().isNotFound())
                // la risposta deve comunque rispettare l'envelope
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Autore con ID 999 non trovato"))
                // in caso di errore senza data, il nodo $.data NON deve essere presente
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ------------------------------------------------------------------ //
    //  POST /autori/add – salvataggio autore valido                       //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /autori/add – 200 OK: autore valido viene salvato e restituito")
    void addAutore_validPayload_shouldReturn200WithSavedAutore() throws Exception {

        AutoreDTO saved = new AutoreDTO(10, "Giovanni", "Verga");
        when(autoreService.save(any(AutoreDTO.class))).thenReturn(saved);

        String json = """
                {
                    "nome": "Giovanni",
                    "cognome": "Verga"
                }
                """;

        mockMvc.perform(post("/autori/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Autore creato con successo"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.nome").value("Giovanni"))
                .andExpect(jsonPath("$.data.cognome").value("Verga"));
    }

    // ------------------------------------------------------------------ //
    //  POST /autori/add – payload invalido (nome blank)                   //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /autori/add – 400 Bad Request: nome blank fallisce la validazione")
    void addAutore_blankNome_shouldReturn400WithValidationError() throws Exception {

        String json = """
                {
                    "nome": "",
                    "cognome": "Manzoni"
                }
                """;

        mockMvc.perform(post("/autori/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                // il nodo data deve contenere la mappa degli errori di campo
                .andExpect(jsonPath("$.data.nome").value("Il nome è obbligatorio"));
    }

    // ------------------------------------------------------------------ //
    //  DELETE /autori/{id} – eliminazione autore esistente                //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("DELETE /autori/{id} – 200 OK: autore eliminato correttamente")
    void deleteAutore_existingId_shouldReturn200WithSuccessMessage() throws Exception {

        // deleteById non lancia eccezioni → non serve stubbing aggiuntivo (void mock)

        mockMvc.perform(delete("/autori/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Autore eliminato correttamente"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ------------------------------------------------------------------ //
    //  DELETE /autori/{id} – autore non trovato                          //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("DELETE /autori/{id} – 404 Not Found: autore inesistente restituisce errore strutturato")
    void deleteAutore_notFound_shouldReturn404() throws Exception {

        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Autore", 99))
                .when(autoreService).deleteById(99);

        mockMvc.perform(delete("/autori/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
