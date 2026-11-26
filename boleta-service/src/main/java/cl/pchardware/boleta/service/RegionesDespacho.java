package cl.pchardware.boleta.service;

import cl.pchardware.boleta.exception.BadRequestException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Regiones de despacho y sus comunas. Es la misma lista que usa el checkout del frontend
 * (src/utils/regiones.js): si se agrega una región o comuna, hay que agregarla en ambos lados.
 */
public final class RegionesDespacho {

    public static final String REGION_POR_DEFECTO = "Región Metropolitana de Santiago";
    public static final String COMUNA_POR_DEFECTO = "Santiago";

    private static final Map<String, List<String>> COMUNAS = Map.of(
            "Región Metropolitana de Santiago", List.of(
                    "Cerrillos", "Santiago", "Providencia", "Las Condes", "Maipú", "Ñuñoa", "Puente Alto",
                    "La Florida", "San Bernardo", "Vitacura", "Pudahuel", "Estación Central"),
            "Región de Valparaíso", List.of(
                    "Valparaíso", "Viña del Mar", "Concón", "Quilpué", "Villa Alemana", "Quillota", "San Antonio", "Los Andes"),
            "Región del Biobío", List.of(
                    "Concepción", "Talcahuano", "Hualpén", "San Pedro de la Paz", "Chiguayante", "Coronel", "Los Ángeles"),
            "Región de Coquimbo", List.of("La Serena", "Coquimbo", "Ovalle", "Vicuña", "Illapel"),
            "Región de Los Lagos", List.of("Puerto Montt", "Puerto Varas", "Osorno", "Castro", "Ancud"));

    private RegionesDespacho() {}

    /** Par validado, con los nombres escritos como en el catálogo. */
    public record Destino(String region, String comuna) {}

    /**
     * Valida que la región exista y que la comuna pertenezca a ella (sin distinguir mayúsculas ni espacios
     * al inicio o al final). Lanza 400 si no se cumple.
     */
    public static Destino validar(String region, String comuna) {
        String regionOficial = buscar(COMUNAS.keySet(), region)
                .orElseThrow(() -> new BadRequestException("Región de despacho no válida: " + region + "."));
        String comunaOficial = buscar(COMUNAS.get(regionOficial), comuna)
                .orElseThrow(() -> new BadRequestException(
                        "La comuna " + comuna + " no pertenece a la " + regionOficial + "."));
        return new Destino(regionOficial, comunaOficial);
    }

    private static Optional<String> buscar(Collection<String> opciones, String valor) {
        if (valor == null) return Optional.empty();
        String v = valor.trim();
        return opciones.stream().filter(o -> o.equalsIgnoreCase(v)).findFirst();
    }
}
