package com.messenger.handler.contatos;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.shared.auth.TokenValidator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ContatoBloqueioHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566")) // LocalStack
            .region(Region.US_EAST_1)
            //   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();

    private static final String TABELA = "contatos";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {

        String authHeader = req.getHeaders() != null ? req.getHeaders().get("Authorization") : null;

        if (!tokenValidator.isTokenValid(authHeader)) {
            return buildResponse(401, "Token inválido ou ausente");
        }

        String usuarioEmail = tokenValidator.getEmailFromToken(authHeader);

        try {
            Map<String, Object> bodyMap = mapper.readValue(req.getBody(), Map.class);
            String contatoEmail = (String) bodyMap.get("contatoEmail");
            Boolean bloquear = (Boolean) bodyMap.get("bloquear");

            if (contatoEmail == null || bloquear == null) {
                return buildResponse(400, "Parâmetros obrigatórios: contatoEmail, bloquear");
            }

            // Verifica se o contato existe
            Map<String, AttributeValue> chave = new HashMap<>();
            chave.put("usuario_email", AttributeValue.fromS(usuarioEmail));
            chave.put("contato_email", AttributeValue.fromS(contatoEmail));

            GetItemRequest getRequest = GetItemRequest.builder()
                    .tableName(TABELA)
                    .key(chave)
                    .build();

            GetItemResponse getResponse = dynamoDb.getItem(getRequest);

            if (!getResponse.hasItem()) {
                return buildResponse(404, "Contato não encontrado");
            }


            // Atualiza o campo bloqueado
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(TABELA)
                    .key(chave)
                    .updateExpression("SET bloqueado = :bloqueado")
                    .expressionAttributeValues(Map.of(
                            ":bloqueado", AttributeValue.fromBool(bloquear)
                    ))
                    .build();

            dynamoDb.updateItem(updateRequest);


            String acao = bloquear ? "bloqueado" : "desbloqueado";

            return buildResponse(200, "Contato " + acao + " com sucesso");

        } catch (Exception e) {
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

