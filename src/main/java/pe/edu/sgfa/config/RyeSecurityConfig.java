package pe.edu.sgfa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class RyeSecurityConfig {

    @Bean
    public PasswordEncoder ryePasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
