package com.bridge.facturacion.notacredito;

import com.bridge.facturacion.notacredito.dto.NotaCreditoRequestDTO;
import com.bridge.facturacion.notacredito.dto.NotaCreditoResponseDTO;
import com.bridge.facturacion.pdf.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class NotaCreditoController {

    private final NotaCreditoService notaCreditoService;
    private final PdfService pdfService;

    /** Crea y emite la NC que anula la factura (body: motivo). */
    @PostMapping("/facturas/{facturaId}/nota-credito")
    public ResponseEntity<NotaCreditoResponseDTO> crearYEmitir(
            @PathVariable Long facturaId,
            @Valid @RequestBody NotaCreditoRequestDTO request) {
        NotaCreditoResponseDTO nc = notaCreditoService.crearYEmitir(facturaId, request.getMotivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(nc);
    }

    /** La NC de una factura (para el link desde una factura ANULADA). */
    @GetMapping("/facturas/{facturaId}/nota-credito")
    public ResponseEntity<NotaCreditoResponseDTO> deFactura(@PathVariable Long facturaId) {
        return ResponseEntity.ok(notaCreditoService.findByFacturaId(facturaId));
    }

    @GetMapping("/notas-credito/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        NotaCredito nc = pdfService.buscarNotaCreditoEmitida(id);
        byte[] pdf = pdfService.generar(nc);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition",
                        "attachment; filename=\"" + pdfService.nombreArchivo(nc) + "\"")
                .body(pdf);
    }
}
