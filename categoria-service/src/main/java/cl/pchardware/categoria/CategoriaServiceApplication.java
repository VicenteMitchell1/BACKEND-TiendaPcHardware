package cl.pchardware.categoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CategoriaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CategoriaServiceApplication.class, args);
    }
}
