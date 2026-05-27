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
        List<AlumnoResponseDTO> allAlumnos = alumnoService.getAllAlumnos();
        return ResponseEntity.status(HttpStatus.OK).body(allAlumnos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponseDTO> findById(@PathVariable Long id) {
        AlumnoResponseDTO alumno = alumnoService.getAlumnoById(id);
        return ResponseEntity.status(HttpStatus.OK).body(alumno);
    }

    @PostMapping
    public ResponseEntity<AlumnoResponseDTO> createAlumno(@Valid @RequestBody AlumnoRequestDTO requestDTO){
        AlumnoResponseDTO alumnoResponseDTO = alumnoService.createAlumno(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(alumnoResponseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponseDTO>  update(@PathVariable Long id, @Valid @RequestBody AlumnoRequestDTO requestDTO){
        AlumnoResponseDTO alumnoResponseDTO = alumnoService.updateAlumno(id, requestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(alumnoResponseDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlumno(@PathVariable Long id){
        alumnoService.deleteAlumnoById(id);
    }

}
