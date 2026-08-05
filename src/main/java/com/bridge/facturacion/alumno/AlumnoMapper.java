package com.bridge.facturacion.alumno;

import com.bridge.facturacion.alumno.dto.AlumnoRequestDTO;
import com.bridge.facturacion.alumno.dto.AlumnoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AlumnoMapper {

    Alumno toEntity(AlumnoRequestDTO dto);

    AlumnoResponseDTO toResponse(Alumno alumno);

    void updateEntityFromDto(AlumnoRequestDTO dto, @MappingTarget Alumno alumno);

}
