package com.bridge.facturacion.emisor;

import com.bridge.facturacion.emisor.dto.EmisorResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmisorMapper {
    EmisorResponseDTO toResponse(Emisor emisor);
}
