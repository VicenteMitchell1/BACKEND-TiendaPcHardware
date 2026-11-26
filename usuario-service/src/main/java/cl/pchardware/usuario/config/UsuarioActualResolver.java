package cl.pchardware.usuario.config;

import cl.pchardware.usuario.dto.UsuarioActual;
import cl.pchardware.usuario.exception.BadRequestException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Inyecta {@link UsuarioActual} en los controladores leyendo SÓLO las cabeceras X-User-* que agrega el gateway
 * (nunca parámetros de la URL ni del cuerpo).
 */
@Configuration
public class UsuarioActualResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UsuarioActual.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String id = request.getHeader("X-User-Id");
        Long idUsuario = null;
        if (StringUtils.hasText(id)) {
            try {
                idUsuario = Long.valueOf(id.trim());
            } catch (NumberFormatException e) {
                throw new BadRequestException("Cabecera X-User-Id inválida.");
            }
        }
        return new UsuarioActual(idUsuario, request.getHeader("X-User-Email"), request.getHeader("X-User-Role"));
    }
}
