package com.bridge.facturacion.factura;

import com.bridge.facturacion.factura.dto.FacturaRequestDTO;
import com.bridge.facturacion.factura.dto.FacturaResponseDTO;
import com.bridge.facturacion.pdf.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final PdfService pdfService;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        var factura = pdfService.buscarEmitida(id);
        byte[] pdf = pdfService.generar(factura);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition",
                        "attachment; filename=\"" + pdfService.nombreArchivo(factura) + "\"")
                .body(pdf);
    }

    @PostMapping
    public ResponseEntity<FacturaResponseDTO> create(@Valid @RequestBody FacturaRequestDTO facturaRequestDTO) {
        FacturaResponseDTO facturaResponseDTO = facturaService.create(facturaRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaResponseDTO);
    }

    @PostMapping("/{id}/emitir")
    public ResponseEntity<FacturaResponseDTO> emitir(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.emitir(id));
    }

    @PostMapping("/emitir")
    public ResponseEntity<List<FacturaResponseDTO>> emitirPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodo) {
        return ResponseEntity.ok(facturaService.emitirPorPeriodo(periodo));
    }

    @GetMapping
    public ResponseEntity<List<FacturaResponseDTO>> findAll() {
        List<FacturaResponseDTO> facturas = facturaService.findAll();
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponseDTO>  findById(@PathVariable Long id) {
        FacturaResponseDTO factura = facturaService.findById(id);
        return ResponseEntity.ok(factura);
    }

    @GetMapping("/por-periodo")
    public ResponseEntity<List<FacturaResponseDTO>> findByPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodo){
        List<FacturaResponseDTO> facturas = facturaService.findByPeriodo(periodo);
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/por-alumno")
    public ResponseEntity<List<FacturaResponseDTO>>  findByAlumno(@RequestParam Long alumnoId){
        List<FacturaResponseDTO> facturas = facturaService.findByAlumno(alumnoId);
        return ResponseEntity.ok(facturas);
    }

}
