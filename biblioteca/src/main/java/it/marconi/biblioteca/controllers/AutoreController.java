package it.marconi.biblioteca.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.APIResponse;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.exceptions.ResourceNotFoundException;
import it.marconi.biblioteca.services.AutoreService;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/autori")
public class AutoreController {
    
    @Autowired
    LibroService libroService;

    @Autowired
    AutoreService autoreService;

    @GetMapping
    @Operation(summary = "Recupera tutti gli autori")
    public APIResponse<List<AutoreDTO>> getAll() {
        List<AutoreDTO> autori = autoreService.findAll();
        return APIResponse.ok(autori, "Lista autori recuperata con successo");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Cerca un autore dato il suo ID")
    public APIResponse<AutoreDTO> getAutore(@PathVariable Integer id) {
        AutoreDTO autore = autoreService.getById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Autore", id));

        return APIResponse.ok(autore, "Autore trovato");
    }

    @GetMapping("/{id}/libri")
    @Operation(summary = "Recupera tutti i libri di un dato autore")
    public APIResponse<List<LibroDTO>> getLibriByAutore(@PathVariable Integer id) {
        List<LibroDTO> libri = libroService.getByAutoreId(id);
        return APIResponse.ok(libri, "Libri dell'autore recuperati con successo");
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo autore")
    public APIResponse<AutoreDTO> addAutore(@Valid @RequestBody AutoreDTO autore) {
        AutoreDTO salvato = autoreService.save(autore);
        return APIResponse.ok(salvato, "Autore creato con successo");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rimuove un autore dal database, e anche tutti i suoi libri")
    public APIResponse<String> deleteAutore(@PathVariable Integer id) {
        boolean deleted = autoreService.deleteById(id);

        if (!deleted) {
            throw new ResourceNotFoundException("Autore", id);
        }

        return APIResponse.success("Autore eliminato correttamente");
    }
}
