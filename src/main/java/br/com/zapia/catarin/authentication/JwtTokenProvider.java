package br.com.zapia.catarin.authentication;

import br.com.zapia.catarin.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.catarin.modelo.Usuario;
import br.com.zapia.catarin.servicos.UsuariosService;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Autowired
    private UsuariosService usuariosService;

    public String generateToken(Authentication authentication) {

        UsuarioAuthentication userPrincipal = (UsuarioAuthentication) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUuid().toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, "Zapia845689!@#$")
                .compact();
    }

    public void validateRequest(HttpServletRequest request) {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && isValidToken(jwt)) {
                UUID uuid = getUserUUIDFromJWT(jwt);
                Usuario userDetails = usuariosService.buscar(uuid);
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, Arrays.asList(userDetails.getPermissao()));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    UsuarioScopedContext.setUsuario(userDetails);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
    }

    public boolean validateTokenWs(String jwt) {
        if (StringUtils.hasText(jwt) && isValidToken(jwt)) {
            UUID uuid = getUserUUIDFromJWT(jwt);
            Usuario userDetails = usuariosService.buscar(uuid);
            if (userDetails != null) {
                UsuarioScopedContext.setUsuario(userDetails);
                return true;
            }
        }
        return false;
    }

    public UUID getUserUUIDFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey("Zapia845689!@#$")
                .parseClaimsJws(token)
                .getBody();

        return UUID.fromString(claims.getSubject());
    }

    public String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || bearerToken.isEmpty()) {
            String token = request.getParameter("token");
            if (token != null && !token.isEmpty()) {
                return token;
            }
        }
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean isValidToken(String authToken) {
        try {
            Jwts.parser().setSigningKey("Zapia845689!@#$").parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: " + authToken);
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: " + authToken);
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: " + authToken);
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: " + authToken);
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}
