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
import java.util.*;
import java.util.stream.Collectors;

public class ContatosListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String TABELA_CONTATOS = "contatos";
    private static final String TABELA_USUARIOS = "usuarios";
    private static final String S3_LOCAL_URL = "http://localhost:4566";
    private static final String BUCKET = "fotos-perfil";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        String authHeader = Optional.ofNullable(request.getHeaders()).map(h -> h.get("Authorization")).orElse(null);

        if (!tokenValidator.isTokenValid(authHeader)) {
            return buildResponse(401, "Token inválido ou ausente");
        }

        String emailLogado = tokenValidator.getEmailFromToken(authHeader);

        try {
            // 1. Consultar contatos do usuário logado
            QueryRequest query = QueryRequest.builder()
                    .tableName(TABELA_CONTATOS)
                    .keyConditionExpression("usuario_email = :usuarioEmail")
                    .filterExpression("aceito = :aceito_true")
                    .expressionAttributeValues(Map.of(
                            ":usuarioEmail", AttributeValue.fromS(emailLogado),
                            ":aceito_true", AttributeValue.fromBool(true)
                    ))
                    .build();

            QueryResponse contatosResponse = dynamoDb.query(query);

            if (contatosResponse.count() == 0) {
                return buildResponse(200, "[]");
            }

            List<String> emailsContatos = contatosResponse.items().stream()
                    .map(item -> item.get("contato_email").s())
                    .toList();

            // Mapa bloqueado dos contatos (para devolver junto)
            Map<String, Boolean> bloqueadosMap = new HashMap<>();
            contatosResponse.items().forEach(item -> {
                Boolean bloqueado = item.containsKey("bloqueado") && item.get("bloqueado").bool();
                bloqueadosMap.put(item.get("contato_email").s(), bloqueado);
            });

            // 2. Preparar BatchGetItem para buscar dados dos contatos
            List<Map<String, AttributeValue>> keys = emailsContatos.stream()
                    .map(email -> Map.of("id", AttributeValue.fromS(email)))
                    .toList();

            BatchGetItemRequest batchRequest = BatchGetItemRequest.builder()
                    .requestItems(Map.of(
                            TABELA_USUARIOS,
                            KeysAndAttributes.builder().keys(keys).build()
                    ))
                    .build();

            BatchGetItemResponse batchResponse = dynamoDb.batchGetItem(batchRequest);

            List<Map<String, AttributeValue>> usuarios = batchResponse.responses().get(TABELA_USUARIOS);

            List<Map<String, String>> resultado = new ArrayList<>();

            for (Map<String, AttributeValue> item : usuarios) {
                String email = item.get("email").s();
                String nomePerfil = item.getOrDefault("nomePerfil", AttributeValue.fromS("")).s();
                String frasePerfil = item.getOrDefault("frasePerfil", AttributeValue.fromS("")).s();
                String fotoKey = item.getOrDefault("fotoPerfil", AttributeValue.fromS(null)).s();
                Boolean bloqueado = bloqueadosMap.getOrDefault(email, AttributeValue.fromBool(false).bool());

                String fotoUrl = null;
                if (fotoKey != null && !fotoKey.isEmpty()) {
                    fotoUrl = String.format("%s/%s/%s", S3_LOCAL_URL, BUCKET, fotoKey);
                }

                resultado.add(Map.of(
                        "email", email,
                        "nomePerfil", nomePerfil,
                        "frasePerfil", frasePerfil,
                        "fotoPerfilUrl", fotoUrl != null ? fotoUrl : "",
                        "bloqueado", bloqueado ? "true" : "false"
                ));
            }

            return buildResponse(200, objectMapper.writeValueAsString(resultado));

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "Erro ao listar contatos: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}
