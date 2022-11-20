package com.trodix.signature.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.SetUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.shaded.json.JSONObject;

/**
 * <p>
 * Convert Keycloak realm client roles to spring roles.
 * </p>
 *
 * <p>
 * Roles are mapped <code>to ROLE_client-id_client-role-name</code>
 * </p>
 *
 * <p>
 * The property app.keycloak.jwt.role.locations holds the list of keycloak client ids where the
 * mapper will extract the roles from.
 * </p>
 *
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(final Jwt source) {
        return new JwtAuthenticationToken(source, Stream.concat(new JwtGrantedAuthoritiesConverter().convert(source)
                .stream(), extractResourceRoles(source).stream())
                .collect(Collectors.toSet()));
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(final Jwt jwt) {
        final Map<String, JSONObject> resourceAccess = new HashMap<>(jwt.getClaim("resource_access"));
        final List<String> resourceRoles = new ArrayList<>();

        resourceAccess.forEach((resource, resourceValue) -> {
            final List<String> roles = (List) resourceValue.get("roles");
            resourceRoles.addAll(roles);
        });

        return resourceRoles.isEmpty() ? SetUtils.emptySet()
                : resourceRoles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toSet());
    }

}
