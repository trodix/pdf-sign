package com.trodix.signature.security.filters;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import com.trodix.signature.security.utils.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that denies all request that not contains a Jwt with an email claim
 */
@Configuration
@Slf4j
public class MandatoryJwtEmailClaimSecurityFilter extends HttpFilter {

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!jwt.hasClaim(Claims.EMAIL.value) || StringUtils.isBlank(jwt.getClaim(Claims.EMAIL.value))) {
            final String msg = "The JWT token did not contained the mandatory " + Claims.EMAIL.value + " claim.";
            log.info(msg);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        log.debug("Claim {} found.", Claims.EMAIL.value);

        chain.doFilter(request, response);
    }

}
