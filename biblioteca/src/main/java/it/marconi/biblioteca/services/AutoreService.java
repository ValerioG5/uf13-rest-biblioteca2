package it.marconi.biblioteca.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.marconi.biblioteca.domain.Autore;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.AutoreMapper;
import it.marconi.biblioteca.exceptions.ResourceNotFoundException;
import it.marconi.biblioteca.repositories.AutoreRepository;

@Service
public class AutoreService {
    
    @Autowired  // dependency injection
    private AutoreRepository autoreRepo;

    @Autowired
    private AutoreMapper mapper;

    public AutoreDTO save(AutoreDTO autore) {

        Autore entity = mapper.toEntity(autore);
        entity.setId(null);     // cancello ID dalle API
        entity = autoreRepo.save(entity);
        return mapper.toDto(entity);
    }

    public List<AutoreDTO> findAll() {
                                                    // mapper::toDto
        return autoreRepo.findAll().stream().map(autore -> mapper.toDto(autore)).toList();
    }

    public AutoreDTO getById(int id) {
        return autoreRepo.findById(id)
            .map(autore -> mapper.toDto(autore))
            .orElseThrow(() -> new ResourceNotFoundException("Autore", id));
    }

    public void deleteById(int id) {
        if (!autoreRepo.existsById(id)) {
            throw new ResourceNotFoundException("Autore", id);
        }
        autoreRepo.deleteById(id);
    }
}
