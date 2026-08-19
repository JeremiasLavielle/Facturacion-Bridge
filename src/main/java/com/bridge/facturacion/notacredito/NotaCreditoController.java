package com.bridge.facturacion.notacredito;

import com.bridge.facturacion.notacredito.dto.NotaCreditoRequestDTO;
import com.bridge.facturacion.notacredito.dto.NotaCreditoResponseDTO;
import com.bridge.facturacion.pdf.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class NotaCreditoController {

    private final NotaCreditoService notaCreditoService;
    private final PdfService pdfService;

@PostMapping("/facturas/{facturaId}/nota-credito")
    public ResponseEntity<NotaCreditoResponseDTO> crearYEmitir(
            @PathVariable Long facturaId,
            @Valid @RequestBody NotaCreditoRequestDTO request) {
        NotaCreditoResponseDTO nc = notaCreditoService.crearYEmitir(facturaId, request.getMotivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(nc);
    }

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
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(pdfService.nombreArchivo(nc), StandardCharsets.UTF_8)
                                .build().toString())
                .body(pdf);
    }
}
