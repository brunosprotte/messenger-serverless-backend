package com.messenger.handler.contatos;

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
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class ContatoDeleteHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String TABELA = "contatos";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        String authHeader = Optional.ofNullable(request.getHeaders()).map(h -> h.get("Authorization")).orElse(null);

        if (!tokenValidator.isTokenValid(authHeader)) {
            return buildResponse(401, "Token inválido ou ausente");
        }

        String usuarioEmail = tokenValidator.getEmailFromToken(authHeader);

        try {
            JsonNode body = objectMapper.readTree(request.getBody());
            String contatoEmail = body.get("contatoEmail").asText();

            // Verifica se o contato existe
            GetItemRequest getReq = GetItemRequest.builder()
                    .tableName(TABELA)
                    .key(Map.of(
                            "usuario_email", AttributeValue.fromS(usuarioEmail),
                            "contato_email", AttributeValue.fromS(contatoEmail)
                    ))
                    .build();

            GetItemResponse getResp = dynamoDb.getItem(getReq);

            if (!getResp.hasItem()) {
                return buildResponse(404, "Contato não encontrado");
            }

            // Deleta o contato
            DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                    .tableName(TABELA)
                    .key(Map.of(
                            "usuario_email", AttributeValue.fromS(usuarioEmail),
                            "contato_email", AttributeValue.fromS(contatoEmail)
                    ))
                    .build();

            dynamoDb.deleteItem(deleteReq);

            return buildResponse(200, "Contato deletado com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "Erro ao deletar contato: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}

