package com.messenger.shared.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

public class TokenValidator {

    private static final String SECRET = "7ce86ced-b98f-4ff0-8366-f27b0ffcdc48";

    public boolean isTokenValid(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("auth header not present or invalid");
            return false;
        }

        String token = authHeader.substring(7); // remove "Bearer "

        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("local-auth0")
                    .build();

            verifier.verify(token);

            return true;

        } catch (JWTVerificationException ex) {
            System.out.println("token validation error: " + ex);
            return false;
        }
    }

}
