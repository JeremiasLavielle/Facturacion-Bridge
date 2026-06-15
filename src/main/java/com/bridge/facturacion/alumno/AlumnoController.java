package com.bridge.facturacion.alumno;


import com.bridge.facturacion.alumno.dto.AlumnoRequestDTO;
import com.bridge.facturacion.alumno.dto.AlumnoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService alumnoService;

    @GetMapping
    public ResponseEntity<List<AlumnoResponseDTO>> findAll() {
        List<AlumnoResponseDTO> allAlumnos = alumnoService.findAll();
        return ResponseEntity.ok(allAlumnos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponseDTO> findById(@PathVariable Long id) {
        AlumnoResponseDTO alumno = alumnoService.findById(id);
        return ResponseEntity.ok(alumno);
    }

    @PostMapping
    public ResponseEntity<AlumnoResponseDTO> create(@Valid @RequestBody AlumnoRequestDTO requestDTO){
        AlumnoResponseDTO alumnoResponseDTO = alumnoService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(alumnoResponseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponseDTO>  update(@PathVariable Long id, @Valid @RequestBody AlumnoRequestDTO requestDTO){
        AlumnoResponseDTO alumnoResponseDTO = alumnoService.update(id, requestDTO);
        return ResponseEntity.ok(alumnoResponseDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id){
        alumnoService.deleteById(id);
    }

}
