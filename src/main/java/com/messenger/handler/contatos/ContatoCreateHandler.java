package com.messenger.handler.contatos;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.shared.auth.TokenValidator;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class ContatoCreateHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TokenValidator tokenValidator = new TokenValidator();
    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566")) // LocalStack
            .region(Region.US_EAST_1)
         //   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();

    private static final String TABELA_USUARIOS = "usuarios";
    private static final String TABELA_CONTATOS = "contatos";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context context) {
        try {
            // Validação do token
            String authHeader = Optional.ofNullable(req.getHeaders()).map(h -> h.get("Authorization")).orElse(null);
            if (authHeader == null || !tokenValidator.isTokenValid(authHeader)) {
                return buildResponse(401, "Token inválido ou ausente");
            }

            String usuarioId = tokenValidator.getEmailFromToken(authHeader);

            // Parse do body
            JsonNode body = mapper.readTree(req.getBody());
            String contatoEmail = body.get("email").asText();

//            if (usuarioId.equalsIgnoreCase(contatoEmail)) {
//                return buildResponse(400, "Não é possível adicionar você mesmo como contato");
//            }

            // Verifica se contato existe na tabela de usuários
            if (!usuarioExiste(contatoEmail)) {
                return buildResponse(404, "Usuário de destino não existe");
            }

            // Verifica duplicidade
            if (contatoJaExiste(usuarioId, contatoEmail)) {
                return buildResponse(409, "Contato já adicionado");
            }

            // Salva o contato
            salvarContato(usuarioId, contatoEmail);
            return buildResponse(201, "Contato adicionado com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(400, "Erro ao processar requisição: " + e.getMessage());
        }
    }

    private boolean usuarioExiste(String email) {
        Map<String, AttributeValue> chave = Map.of("id", AttributeValue.fromS(email));
        GetItemRequest req = GetItemRequest.builder().tableName(TABELA_USUARIOS).key(chave).build();
        return dynamoDb.getItem(req).hasItem();
    }

    private boolean contatoJaExiste(String usuarioId, String contatoEmail) {
        Map<String, AttributeValue> chave = Map.of(
                "usuario_email", AttributeValue.fromS(usuarioId),
                "contato_email", AttributeValue.fromS(contatoEmail)
        );
        GetItemRequest req = GetItemRequest.builder()
                .tableName(TABELA_CONTATOS)
                .key(chave)
                .build();
        return dynamoDb.getItem(req).hasItem();
    }

    private void salvarContato(String usuarioId, String contatoEmail) {
        String createdAt = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Map<String, AttributeValue> item = Map.of(
                "usuario_email", AttributeValue.fromS(usuarioId),
                "contato_email", AttributeValue.fromS(contatoEmail),
                "bloqueado", AttributeValue.fromBool(false),
                "createdAt", AttributeValue.fromS(createdAt)
        );
        PutItemRequest req = PutItemRequest.builder()
                .tableName(TABELA_CONTATOS)
                .item(item)
                .build();
        dynamoDb.putItem(req);
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}

