package com.bridge.facturacion.alumno;

import com.bridge.facturacion.alumno.dto.AlumnoRequestDTO;
import com.bridge.facturacion.alumno.dto.AlumnoResponseDTO;
import com.bridge.facturacion.alumno.exception.AlumnoAlreadyExistsException;
import com.bridge.facturacion.alumno.exception.AlumnoNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AlumnoService {

    private final AlumnoRepository repository;
    private final AlumnoMapper mapper;

    public AlumnoResponseDTO create(AlumnoRequestDTO requestDTO) {
        if (repository.findByDni(requestDTO.getDni()).isPresent()) {
            throw new AlumnoAlreadyExistsException(requestDTO.getDni());
        }
        Alumno alumno = mapper.toEntity(requestDTO);
        alumno = repository.save(alumno);
        return mapper.toResponse(alumno);
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponseDTO> findAll() {
        List<Alumno> alumnos = repository.findAll();
        return alumnos.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AlumnoResponseDTO findById(Long id) {
        Alumno alumno = repository.findById(id)
                .orElseThrow(() -> new AlumnoNotFoundException(id));
        return mapper.toResponse(alumno);
    }

    public AlumnoResponseDTO update(Long id, AlumnoRequestDTO requestDTO) {
        Alumno alumno = repository.findById(id)
                .orElseThrow(() -> new AlumnoNotFoundException(id));
        mapper.updateEntityFromDto(requestDTO, alumno);
        repository.save(alumno);
        return mapper.toResponse(alumno);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new AlumnoNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
