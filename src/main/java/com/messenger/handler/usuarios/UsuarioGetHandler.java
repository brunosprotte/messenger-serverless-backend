package com.messenger.handler.usuarios;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.shared.auth.TokenValidator;
import com.messenger.shared.model.Usuario;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class UsuarioGetHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String TABELA = "usuarios";
    private static final String EMAIL_INDEX = "email-index";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String authHeader = Optional.ofNullable(request.getHeaders()).map(h -> h.get("Authorization")).orElse(null);

        if (!tokenValidator.isTokenValid(authHeader)) {
            return buildResponse(401, "Token inválido ou ausente");
        }

        String email = tokenValidator.getEmailFromToken(authHeader);

        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(TABELA)
                    .indexName(EMAIL_INDEX)
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(email)))
                    .limit(1)
                    .build();

            QueryResponse response = dynamoDb.query(queryRequest);

            if (response.count() == 0) {
                return buildResponse(404, "Usuário não encontrado");
            }

            Map<String, AttributeValue> item = response.items().get(0);
            Usuario usuario = new Usuario(
                    item.get("id").s(),
                    item.getOrDefault("nome", AttributeValue.fromS("")).s(),
                    null, // nunca retornamos senha!
                    item.get("email").s(),
                    item.get("fotoPerfil").s(),
                    item.get("nomePerfil").s(),
                    item.get("frasePerfil").s()
            );

            String json = objectMapper.writeValueAsString(usuario);
            return buildResponse(200, json);

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "Erro ao buscar usuário: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}
