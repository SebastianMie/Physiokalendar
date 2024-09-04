package com.example.physiokalendar.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    // Generiert ein Token für den angegebenen Benutzernamen
    public String generateToken(String username) {
        return buildToken(username);
    }

    // Erstellt ein Token mit dem angegebenen Benutzernamen
    private String buildToken(String username) {
        return Jwts.builder()
                .setSubject(username)  // Setzt das Subject auf den Benutzernamen
                .setIssuedAt(new Date())  // Setzt das Ausstellungsdatum auf jetzt
                .setExpiration(new Date(System.currentTimeMillis() + expiration))  // Setzt das Ablaufdatum
                .signWith(SignatureAlgorithm.HS256, getSignInKey())  // Signiert das Token
                .compact();  // Kompakt formatiert das Token
    }

    // Extrahiert Claims aus einem Token
    // Extrahiert Claims aus einem Token
    public Claims extractClaims(String token) {
    return Jwts.parser()  // Initialisiert den Parser
            .setSigningKey(getSignInKey())  // Setzt den Signaturschlüssel
            .parseClaimsJws(token)  // Parst das Token
            .getBody();  // Gibt die Claims zurück
    }


    // Extrahiert den Benutzernamen aus einem Token
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    // Überprüft, ob ein Token abgelaufen ist
    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    // Validiert das Token für den angegebenen Benutzernamen
    public boolean validateToken(String token, String username) {
        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }

    // Erstellt den Signaturschlüssel aus dem secretKey
    private Key getSignInKey() {
        return new javax.crypto.spec.SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName());
    }
}
