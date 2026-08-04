package com.bridge.facturacion.notacredito;

import com.bridge.facturacion.emisor.EmisorMapper;
import com.bridge.facturacion.notacredito.dto.NotaCreditoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { EmisorMapper.class })
public interface NotaCreditoMapper {

    @Mapping(target = "facturaId", source = "factura.id")
    NotaCreditoResponseDTO toResponse(NotaCredito notaCredito);
}
