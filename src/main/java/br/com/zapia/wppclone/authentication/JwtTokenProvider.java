package br.com.zapia.wppclone.authentication;

import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.UsuariosService;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Autowired
    private UsuariosService usuariosService;
    @Value("${jwt.sign.pass}")
    private String jwtSignPass;

    @PostConstruct
    public void init() {
        jwtSignPass = Base64.getEncoder().encodeToString(jwtSignPass.getBytes());
    }

    public String generateToken(UsuarioAuthentication userPrincipal) {

        return Jwts.builder()
                .setSubject(userPrincipal.getUsuario().getUuid().toString())
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS512, jwtSignPass)
                .compact();
    }

    public void validateRequest(HttpServletRequest request) {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && isValidToken(jwt)) {
                UUID uuid = getUserUUIDFromJWT(jwt);
                Usuario userDetails = usuariosService.buscar(uuid);
                if (userDetails != null && userDetails.isAtivo()) {
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
                .setSigningKey(jwtSignPass)
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
            Jwts.parser().setSigningKey(jwtSignPass).parseClaimsJws(authToken);
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
