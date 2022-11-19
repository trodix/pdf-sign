package com.trodix.signature.filters;

import java.util.Optional;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import io.quarkus.runtime.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class SecurityInterceptor {

    private final JsonWebToken jwt;

    public SecurityInterceptor(final JsonWebToken jwt) {
        this.jwt = jwt;
    }

    @ServerRequestFilter(preMatching = true)
    public Optional<RestResponse<Void>> check() {

        if (!this.jwt.containsClaim(Claims.email.name()) || StringUtil.isNullOrEmpty(this.jwt.getClaim(Claims.email))) {
            final String msg = "The JWT token did not contained the mandatory " + Claims.email + " claim.";
            log.info(msg);
            return Optional.of(RestResponse.status(StatusCode.UNAUTHORIZED, msg));
        }

        log.debug(Claims.email + " claim found");

        return Optional.empty();
    }

}
