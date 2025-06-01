package com.messenger.handler.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.messenger.shared.auth.TokenValidator;

import java.util.Map;

public class AuthHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final TokenValidator tokenValidator = new TokenValidator();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        Map<String, String> headers = input.getHeaders();
        String authHeader = headers != null ? headers.get("Authorization") : null;

        System.out.println("token: " + authHeader);

        if (authHeader == null || !tokenValidator.isTokenValid(authHeader)) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Token ausente ou inválido");
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(200);

    }
}