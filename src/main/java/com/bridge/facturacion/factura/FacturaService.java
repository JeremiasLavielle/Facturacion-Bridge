package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.AlumnoRepository;
import com.bridge.facturacion.alumno.exception.AlumnoNotFoundException;
import com.bridge.facturacion.factura.dto.FacturaRequestDTO;

public class FacturaService {

    private final AlumnoRepository alumnoRepository;

    public FacturaService(AlumnoRepository repository) {
        this.alumnoRepository = repository;
    }

    private Alumno findAlumnoById(FacturaRequestDTO facturaRequestDTO) {
        Long id = facturaRequestDTO.getAlumnoId();
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new AlumnoNotFoundException(id));
    }

}
