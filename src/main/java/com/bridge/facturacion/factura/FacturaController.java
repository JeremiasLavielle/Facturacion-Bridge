package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.factura.dto.FacturaRequestDTO;
import com.bridge.facturacion.factura.dto.FacturaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @PostMapping
    public ResponseEntity<FacturaResponseDTO> crearFactura(@Valid @RequestBody FacturaRequestDTO facturaRequestDTO) {
        FacturaResponseDTO facturaResponseDTO = facturaService.crearFactura(facturaRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaResponseDTO);
    }

    @GetMapping
    public ResponseEntity<List<FacturaResponseDTO>> obtenerFacturas() {
        List<FacturaResponseDTO> facturas = facturaService.getAllFacturas();
        return ResponseEntity.status(HttpStatus.OK).body(facturas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponseDTO>  obtenerFactura(@PathVariable Long id) {
        FacturaResponseDTO factura = facturaService.getFacturaById(id);
        return ResponseEntity.status(HttpStatus.OK).body(factura);
    }

    @GetMapping("/por-periodo")
    public ResponseEntity<List<FacturaResponseDTO>> obtenerFacturasByPeriodo(@RequestParam LocalDate periodo){
        List<FacturaResponseDTO> facturas = facturaService.getFacturasByPeriodo(periodo);
        return ResponseEntity.status(HttpStatus.OK).body(facturas);
    }

    @GetMapping("/por-alumno")
    public ResponseEntity<List<FacturaResponseDTO>>  obtenerFacturasByAlumno(@RequestParam Long alumnoId){
        List<FacturaResponseDTO> facturas = facturaService.getFacturasByAlumno(alumnoId);
        return ResponseEntity.status(HttpStatus.OK).body(facturas);
    }

}
