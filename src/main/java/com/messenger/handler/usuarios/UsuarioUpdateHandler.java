package com.messenger.handler.usuarios;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.shared.auth.TokenValidator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.net.URI;
import java.util.*;

public class UsuarioUpdateHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String TABELA = "usuarios";
    private static final String EMAIL_INDEX = "email-index";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        String authHeader = Optional.ofNullable(request.getHeaders()).map(h -> h.get("Authorization")).orElse(null);

        if (!tokenValidator.isTokenValid(authHeader)) {
            return buildResponse(401, "Token inválido ou ausente");
        }

        String emailUsuarioLogado = tokenValidator.getEmailFromToken(authHeader);

        try {
            JsonNode body = objectMapper.readTree(request.getBody());

            // Verifica se o usuario existe
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(TABELA)
                    .indexName(EMAIL_INDEX)
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(emailUsuarioLogado)))
                    .limit(1)
                    .build();

            QueryResponse queryResponse = dynamoDb.query(queryRequest);

            if (queryResponse.count() == 0) {
                return buildResponse(404, "Usuario não encontrado");
            }

            Map<String, AttributeValue> item = queryResponse.items().get(0);
            String usuarioId = item.get("id").s(); // Chave primária real

            boolean hasNomePerfil = body.has("nomePerfil");
            boolean hasFrasePerfil = body.has("frasePerfil");

            StringBuilder updateExpression = new StringBuilder("SET ");
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            Map<String, String> expressionNames = new HashMap<>();
            List<String> updates = new ArrayList<>();

            if (hasNomePerfil) {
                updates.add("#nomePerfil = :nomePerfil");
                expressionValues.put(":nomePerfil", AttributeValue.fromS(body.get("nomePerfil").asText()));
                expressionNames.put("#nomePerfil", "nomePerfil");
            }

            if (hasFrasePerfil) {
                updates.add("#frasePerfil = :frasePerfil");
                expressionValues.put(":frasePerfil", AttributeValue.fromS(body.get("frasePerfil").asText()));
                expressionNames.put("#frasePerfil", "frasePerfil");
            }

            updateExpression.append(String.join(", ", updates));

            // Atualiza o contato
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(TABELA)
                    .key(Map.of("id", AttributeValue.fromS(usuarioId)))
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .build();

            dynamoDb.updateItem(updateRequest);

            return buildResponse(200, "Usuário atualizado com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "Erro ao atualizar contato: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody("{\"message\": \"" + body + "\"}")
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}
