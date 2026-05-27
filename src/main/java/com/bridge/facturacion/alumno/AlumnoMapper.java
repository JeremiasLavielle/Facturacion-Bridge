package com.bridge.facturacion.alumno;


import com.bridge.facturacion.alumno.dto.AlumnoRequestDTO;
import com.bridge.facturacion.alumno.dto.AlumnoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;


// target/generated-sources/annotations/com/bridge/facturacion/alumno/AlumnoMapperImpl.java --> Ver implementacion

@Mapper(componentModel = "spring")
public interface AlumnoMapper {

    Alumno toEntity(AlumnoRequestDTO dto); // id asignado por la db

    AlumnoResponseDTO toResponse(Alumno alumno);

    void updateEntityFromDto(AlumnoRequestDTO dto, @MappingTarget Alumno alumno);

}
