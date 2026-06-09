package it.marconi.biblioteca.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.APIResponse;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.exceptions.ResourceNotFoundException;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/libri")
public class LibroController {
    
    @Autowired
    LibroService libroService;

    @GetMapping
    @Operation(summary = "Recupera la lista di tutti i libri")
    public APIResponse<List<LibroDTO>> getAll() {
        List<LibroDTO> libri = libroService.findAll();
        return APIResponse.ok(libri, "Lista libri recuperata con successo");
    }

    @GetMapping("/{isbn}")
    @Operation(summary = "Cerca un libro dal sui ISBN")
    public APIResponse<LibroDTO> getLibroByIsbn(@PathVariable String isbn) {
        LibroDTO libro = libroService.getByIsbn(isbn)
            .orElseThrow(() -> new ResourceNotFoundException("Libro con ISBN " + isbn + " non trovato"));

        return APIResponse.ok(libro, "Libro trovato");
    }

    @GetMapping("/libro")
    @Operation(summary = "Cerca un libro per titolo esatto")
    public APIResponse<LibroDTO> getLibroByTitolo(@RequestParam("titolo") String titolo) {
        LibroDTO libro = libroService.getByTitolo(titolo)
            .orElseThrow(() -> new ResourceNotFoundException("Libro con titolo '" + titolo + "' non trovato"));

        return APIResponse.ok(libro, "Libro trovato");
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo libro, dato l'autore")
    public APIResponse<LibroDTO> addLibro(@Valid @RequestBody LibroDTO libro) {
        LibroDTO salvato = libroService.save(libro);  // Rimuovi orElseThrow()
        return APIResponse.ok(salvato, "Libro creato con successo");
    }

    @DeleteMapping("/{isbn}")
    @Operation(summary = "Elimina un libro dato il suo ISBN")
    public APIResponse<String> deleteLibro(@PathVariable String isbn) {
        boolean deleted = libroService.deleteByIsbn(isbn);

        if (!deleted) {
            throw new ResourceNotFoundException("Libro con ISBN " + isbn + " non trovato");
        }

        return APIResponse.success("Libro eliminato correttamente");
    }
}
