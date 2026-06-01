package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.AlumnoMapper;
import com.bridge.facturacion.factura.dto.FacturaResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = { AlumnoMapper.class })
public interface FacturaMapper {
 FacturaResponseDTO toResponse(Factura factura);
}
