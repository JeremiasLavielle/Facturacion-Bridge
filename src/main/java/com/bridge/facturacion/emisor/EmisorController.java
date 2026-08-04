package com.bridge.facturacion.emisor;

import com.bridge.facturacion.emisor.dto.EmisorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo lectura: pobla el selector de emisor al crear una factura. */
@RestController
@RequestMapping("/emisores")
@RequiredArgsConstructor
public class EmisorController {

    private final EmisorRepository emisorRepository;
    private final EmisorMapper emisorMapper;

    @GetMapping
    public ResponseEntity<List<EmisorResponseDTO>> findActivos() {
        List<EmisorResponseDTO> emisores = emisorRepository.findByActivoTrueOrderById()
                .stream().map(emisorMapper::toResponse).toList();
        return ResponseEntity.ok(emisores);
    }
}
