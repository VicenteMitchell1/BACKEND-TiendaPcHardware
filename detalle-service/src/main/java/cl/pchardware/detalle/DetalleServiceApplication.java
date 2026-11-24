package cl.pchardware.detalle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DetalleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DetalleServiceApplication.class, args);
    }
}
