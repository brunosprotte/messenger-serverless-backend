package com.messenger.handler.contatos;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.shared.auth.TokenValidator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ContatoAceitarHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String TABELA = "contatos";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        String authHeader = request.getHeaders() != null ? request.getHeaders().get("Authorization") : null;

        if (!tokenValidator.isTokenValid(authHeader)) {
            return buildResponse(401, "Token inválido ou ausente");
        }

        String usuarioEmail = tokenValidator.getEmailFromToken(authHeader);

        try {
            // Parse do body (esperado um JSON com "contatoEmail")
            Map<String, Object> bodyMap = mapper.readValue(request.getBody(), Map.class);
            String contatoEmail = (String) bodyMap.get("contatoEmail");
            Boolean aceitar = (Boolean) bodyMap.get("aceitar");

            if (contatoEmail == null || aceitar == null) {
                return buildResponse(400, "Parâmetros obrigatórios: contatoEmail, aceitar");
            }

            // Verifica se o contato existe
            Map<String, AttributeValue> chave = new HashMap<>();
            chave.put("usuario_email", AttributeValue.fromS(contatoEmail));
            chave.put("contato_email", AttributeValue.fromS(usuarioEmail));

            GetItemRequest getRequest = GetItemRequest.builder()
                    .tableName(TABELA)
                    .key(chave)
                    .build();

            GetItemResponse getResponse = dynamoDb.getItem(getRequest);

            if (!getResponse.hasItem()) {
                return buildResponse(404, "Contato não encontrado");
            }

            // Atualiza o campo aceito
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(TABELA)
                    .key(chave)
                    .updateExpression("SET aceito = :aceito")
                    .expressionAttributeValues(Map.of(
                            ":aceito", AttributeValue.fromBool(aceitar)
                    ))
                    .build();

            dynamoDb.updateItem(updateRequest);

            return buildResponse(200, "Contato aceito com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(400, "Erro ao processar requisição: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}
