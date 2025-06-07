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
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class UsuarioCreateHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String TABELA = "usuarios";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {

        Map<String, String> headers = req.getHeaders();
        String authHeader = headers != null ? headers.get("Authorization") : null;

        if (authHeader == null || !tokenValidator.isTokenValid(authHeader)) {
            System.out.println("Token inválido ou ausente");
            return buildResponse(401, "Token inválido ou ausente");
        }

        try {
            Usuario usuario = mapper.readValue(req.getBody(), Usuario.class);

            // Verifica se email já está em uso
            if (emailExiste(usuario.getEmail())) {
                System.out.println("E-mail já está em uso");
                return buildResponse(409, "E-mail já está em uso");
            }

            // Salva usuário no DynamoDB
            salvarUsuario(usuario);

            return buildResponse(201, "Usuário cadastrado com sucesso");

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            return buildResponse(400, "Erro ao processar requisição: " + e.getMessage());
        }
    }

    private boolean emailExiste(String email) {
        Map<String, AttributeValue> chave = Map.of("id", AttributeValue.fromS(email));

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABELA)
                .key(chave)
                .build();

        return dynamoDb.getItem(request).hasItem();
    }

    private void salvarUsuario(Usuario usuario) {
        System.out.println("Salvando usuario");
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(usuario.getEmail()));
        item.put("nome", AttributeValue.fromS(usuario.getNome()));
        item.put("email", AttributeValue.fromS(usuario.getEmail()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABELA)
                .item(item)
                .build();

        dynamoDb.putItem(request);
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}