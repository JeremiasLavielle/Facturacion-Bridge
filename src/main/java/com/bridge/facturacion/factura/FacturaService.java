package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.AlumnoRepository;
import com.bridge.facturacion.alumno.exception.AlumnoNotFoundException;
import com.bridge.facturacion.factura.dto.FacturaRequestDTO;
import com.bridge.facturacion.factura.dto.FacturaResponseDTO;
import com.bridge.facturacion.factura.exception.FacturaDuplicadaException;
import com.bridge.facturacion.factura.exception.FacturaNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class FacturaService {

    private final AlumnoRepository alumnoRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaMapper facturaMapper;

    private Alumno buscarAlumno(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new AlumnoNotFoundException(id));
    }

    public FacturaResponseDTO crearFactura(FacturaRequestDTO facturaRequestDTO) {

        Alumno alumno = buscarAlumno(facturaRequestDTO.getAlumnoId());
        LocalDate periodo = facturaRequestDTO.getPeriodo();

        if (facturaRepository.existsByAlumnoAndPeriodo(alumno, periodo)) {
            throw new FacturaDuplicadaException(alumno, periodo);
        }

        Factura factura = new Factura();

        factura.setAlumno(alumno);
        factura.setMonto(facturaRequestDTO.getMonto());
        factura.setPeriodo(facturaRequestDTO.getPeriodo());
        factura.setEstado(EstadoFactura.PENDIENTE);

        Factura facturaGuardada = facturaRepository.save(factura);
        return facturaMapper.toResponse(facturaGuardada);
    }
    @Transactional(readOnly = true)
    public FacturaResponseDTO getFacturaById(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new FacturaNotFoundException(id));
        return facturaMapper.toResponse(factura);
    }
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> getAllFacturas(){
        List<Factura> facturas = facturaRepository.findAll();
        return facturas.stream().map(facturaMapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> getFacturasByPeriodo(LocalDate periodo) {
        List<Factura> facturas = facturaRepository.findByPeriodo(periodo);
        return facturas.stream().map(facturaMapper::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> getFacturasByAlumno(Long alumnoId) {
        Alumno alumno = buscarAlumno(alumnoId);
        List<Factura> facturas = facturaRepository.findByAlumno(alumno);
        return facturas.stream().map(facturaMapper::toResponse).toList();
    }


}
