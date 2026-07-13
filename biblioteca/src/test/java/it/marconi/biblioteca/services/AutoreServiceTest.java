package it.marconi.biblioteca.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.marconi.biblioteca.domain.Autore;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.AutoreMapper;
import it.marconi.biblioteca.exceptions.ResourceNotFoundException;
import it.marconi.biblioteca.repositories.AutoreRepository;

/**
 * Unit test puri per AutoreService.
 * Il service è isolato dal database e da qualsiasi contesto Spring:
 * - AutoreRepository è sostituito da un @Mock Mockito
 * - AutoreMapper è sostituito da un @Mock Mockito
 * L'unico codice eseguito "per davvero" è quello dentro AutoreService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutoreService – Unit Tests")
class AutoreServiceTest {

    @Mock
    private AutoreRepository autoreRepo;

    @Mock
    private AutoreMapper mapper;

    // @InjectMocks crea l'istanza di AutoreService e vi inietta i mock sopra
    @InjectMocks
    private AutoreService autoreService;

    // ------------------------------------------------------------------ //
    //  save() – regola di business: l'ID deve essere azzerato             //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("save() – l'ID viene azzerato (null) prima del salvataggio nel DB")
    void save_shouldSetIdToNullBeforePersisting() {

        // GIVEN: un DTO con ID forzato dal client (tentativo di corruzione)
        AutoreDTO inputDto = new AutoreDTO(42, "Carlo", "Goldoni");

        // L'entity restituita dal mapper ha ancora l'ID impostato (come farebbe il mapper reale)
        Autore entityWithId = new Autore();
        entityWithId.setId(42);
        entityWithId.setNome("Carlo");
        entityWithId.setCognome("Goldoni");

        // L'entity che il repo restituisce dopo il salvataggio (con ID generato dal DB)
        Autore savedEntity = new Autore();
        savedEntity.setId(1);
        savedEntity.setNome("Carlo");
        savedEntity.setCognome("Goldoni");

        AutoreDTO expectedDto = new AutoreDTO(1, "Carlo", "Goldoni");

        when(mapper.toEntity(inputDto)).thenReturn(entityWithId);
        when(autoreRepo.save(argThat(e -> e.getId() == null))).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(expectedDto);

        // WHEN
        AutoreDTO result = autoreService.save(inputDto);

        // THEN: verifica che il DTO restituito sia quello corretto
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1);
        assertThat(result.nome()).isEqualTo("Carlo");

        // THEN (critico): verifica che il repo.save sia stato chiamato con un'entity
        // che ha ID == null, ovvero la regola di business è stata applicata
        verify(autoreRepo).save(argThat(entity -> {
            assertThat(entity.getId())
                    .as("L'ID deve essere null prima del salvataggio per impedire la corruzione di record esistenti")
                    .isNull();
            return true;
        }));
    }

    @Test
    @DisplayName("save() – anche se il DTO non ha ID, il salvataggio avviene con ID null")
    void save_withNullId_shouldPersistWithNullId() {

        AutoreDTO inputDto = new AutoreDTO(null, "Italo", "Calvino");

        Autore entity = new Autore();
        entity.setId(null);
        entity.setNome("Italo");
        entity.setCognome("Calvino");

        Autore savedEntity = new Autore();
        savedEntity.setId(5);
        savedEntity.setNome("Italo");
        savedEntity.setCognome("Calvino");

        AutoreDTO expectedDto = new AutoreDTO(5, "Italo", "Calvino");

        when(mapper.toEntity(inputDto)).thenReturn(entity);
        when(autoreRepo.save(argThat(e -> e.getId() == null))).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(expectedDto);

        AutoreDTO result = autoreService.save(inputDto);

        assertThat(result.id()).isEqualTo(5);
        verify(autoreRepo).save(argThat(e -> e.getId() == null));
    }

    // ------------------------------------------------------------------ //
    //  findAll() – delega al repository e mappa ogni entity               //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("findAll() – chiama il repository e converte tutte le entity in DTO")
    void findAll_shouldDelegateToRepoAndMapResults() {

        Autore a1 = new Autore();
        a1.setId(1); a1.setNome("Dante"); a1.setCognome("Alighieri");

        Autore a2 = new Autore();
        a2.setId(2); a2.setNome("Francesco"); a2.setCognome("Petrarca");

        AutoreDTO dto1 = new AutoreDTO(1, "Dante", "Alighieri");
        AutoreDTO dto2 = new AutoreDTO(2, "Francesco", "Petrarca");

        when(autoreRepo.findAll()).thenReturn(List.of(a1, a2));
        when(mapper.toDto(a1)).thenReturn(dto1);
        when(mapper.toDto(a2)).thenReturn(dto2);

        List<AutoreDTO> result = autoreService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);
        verify(autoreRepo).findAll();
    }

    // ------------------------------------------------------------------ //
    //  getById() – autore esistente                                        //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("getById() – autore trovato: restituisce il DTO corretto")
    void getById_existingId_shouldReturnDto() {

        Autore entity = new Autore();
        entity.setId(1); entity.setNome("Giovanni"); entity.setCognome("Verga");

        AutoreDTO expectedDto = new AutoreDTO(1, "Giovanni", "Verga");

        when(autoreRepo.findById(1)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(expectedDto);

        AutoreDTO result = autoreService.getById(1);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1);
        assertThat(result.nome()).isEqualTo("Giovanni");
        assertThat(result.cognome()).isEqualTo("Verga");
    }

    // ------------------------------------------------------------------ //
    //  getById() – autore non trovato → ResourceNotFoundException          //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("getById() – ID inesistente: lancia ResourceNotFoundException")
    void getById_notFound_shouldThrowResourceNotFoundException() {

        when(autoreRepo.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> autoreService.getById(999)
        );

        assertThat(ex.getMessage()).contains("999");
        assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        verify(autoreRepo).findById(999);
    }

    // ------------------------------------------------------------------ //
    //  deleteById() – autore esistente                                     //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("deleteById() – autore esistente: invoca deleteById sul repository")
    void deleteById_existingId_shouldCallRepositoryDelete() {

        when(autoreRepo.existsById(1)).thenReturn(true);

        autoreService.deleteById(1);

        verify(autoreRepo).existsById(1);
        verify(autoreRepo).deleteById(1);
        verifyNoMoreInteractions(autoreRepo);
    }

    // ------------------------------------------------------------------ //
    //  deleteById() – autore non trovato → ResourceNotFoundException       //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("deleteById() – ID inesistente: lancia ResourceNotFoundException senza chiamare deleteById")
    void deleteById_notFound_shouldThrowWithoutDeletingAnything() {

        when(autoreRepo.existsById(999)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> autoreService.deleteById(999)
        );

        assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getMessage()).contains("999");

        // il deleteById NON deve essere mai invocato
        verify(autoreRepo).existsById(999);
        verify(autoreRepo, org.mockito.Mockito.never()).deleteById(999);
    }
}
