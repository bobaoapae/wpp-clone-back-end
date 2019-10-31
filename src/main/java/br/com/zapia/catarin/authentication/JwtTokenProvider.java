package br.com.zapia.catarin.authentication;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    public String generateToken(Authentication authentication) {

        UsuarioAuthentication userPrincipal = (UsuarioAuthentication) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUuid().toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, "Zapia845689!@#$")
                .compact();
    }

    public UUID getUserUUIDFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey("Zapia845689!@#$")
                .parseClaimsJws(token)
                .getBody();

        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey("Zapia845689!@#$").parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}
