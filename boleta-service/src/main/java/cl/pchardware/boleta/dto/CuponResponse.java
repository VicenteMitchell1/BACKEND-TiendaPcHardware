package cl.pchardware.boleta.dto;

import cl.pchardware.boleta.model.Cupon;

public record CuponResponse(Long id, String codigo, Integer porcentaje, String descripcion, Boolean activo) {
    public static CuponResponse de(Cupon c) {
        return new CuponResponse(c.getId(), c.getCodigo(), c.getPorcentaje(), c.getDescripcion(), c.getActivo());
    }
}
