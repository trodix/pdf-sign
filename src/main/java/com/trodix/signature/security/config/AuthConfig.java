package com.trodix.signature.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.context.WebApplicationContext;
import com.trodix.signature.security.KeycloakJwtAuthenticationConverter;

@Configuration
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
public class AuthConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    public AuthConfig(final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter) {
        this.keycloakJwtAuthenticationConverter = keycloakJwtAuthenticationConverter;
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {

        // OAUTH authentication
        http.requestMatchers()
                .anyRequest()
                .and()
                .oauth2ResourceServer(
                        config -> config
                                .jwt()
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
                .authorizeRequests()
                .anyRequest()
                .authenticated();

        return http.build();
    }

    /**
     * Add Bean injection capabilities for Jwt
     * 
     * <p>This bean is request scoped, this means that components using this bean must also be request scoped</p>
     * 
     * @return The Jwt associated with the Principal
     */
    @Bean
    @Scope(WebApplicationContext.SCOPE_REQUEST)
    public Jwt jwt() {
        return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}
