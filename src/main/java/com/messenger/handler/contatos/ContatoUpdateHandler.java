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
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ContatoUpdateHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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

        String usuarioLogado = tokenValidator.getEmailFromToken(authHeader);

        try {
            JsonNode body = objectMapper.readTree(request.getBody());
            String usuarioEmail = body.get("usuarioEmail").asText();
            String contatoEmail = body.get("contatoEmail").asText();

            boolean hasAceito = body.has("aceito");
            boolean hasBloqueado = body.has("bloqueado");

            if (!usuarioLogado.equalsIgnoreCase(usuarioEmail) && hasBloqueado) {
                return buildResponse(403, "Você não tem permissão para atualizar este contato.");
            }

            if (usuarioEmail.isEmpty() || contatoEmail.isEmpty() || (!hasAceito && !hasBloqueado)) {
                return buildResponse(400, "Requisição inválida: informe usuarioEmail, contatoEmail e pelo menos um dos campos 'aceito' ou 'bloqueado'");
            }

            Map<String, AttributeValue> chave = new HashMap<>();
            chave.put("usuario_email", AttributeValue.fromS(usuarioEmail));
            chave.put("contato_email", AttributeValue.fromS(contatoEmail));

            // Verifica se o contato existe
            GetItemRequest getRequest = GetItemRequest.builder()
                    .tableName(TABELA)
                    .key(chave)
                    .build();

            GetItemResponse getResponse = dynamoDb.getItem(getRequest);

            if (!getResponse.hasItem()) {
                return buildResponse(404, "Contato não encontrado");
            }

            // Monta a expressão de update dinamicamente
            StringBuilder updateExpression = new StringBuilder("SET ");
            Map<String, AttributeValue> expressionValues = new HashMap<>();

            if (hasAceito) {
                updateExpression.append("aceito = :aceito");
                expressionValues.put(":aceito", AttributeValue.fromBool(body.get("aceito").asBoolean()));
            }

            if (hasBloqueado) {
                if (hasAceito) {
                    updateExpression.append(", ");
                }
                updateExpression.append("bloqueado = :bloqueado");
                expressionValues.put(":bloqueado", AttributeValue.fromBool(body.get("bloqueado").asBoolean()));
            }

            // Atualiza o contato
            UpdateItemRequest updateReq = UpdateItemRequest.builder()
                    .tableName(TABELA)
                    .key(chave)
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeValues(expressionValues)
                    .build();

            dynamoDb.updateItem(updateReq);

            // Se aceitou, criar reciprocidade caso não exista
            if (hasAceito && body.get("aceito").asBoolean()) {
                criarContatoReciprocoSeNecessario(usuarioEmail, contatoEmail);
            }

            return buildResponse(200, "Contato atualizado com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "Erro ao atualizar contato: " + e.getMessage());
        }
    }

    private void criarContatoReciprocoSeNecessario(String usuarioEmail, String contatoEmail) {
        Map<String, AttributeValue> chaveReciproco = Map.of(
                "usuario_email", AttributeValue.fromS(contatoEmail),
                "contato_email", AttributeValue.fromS(usuarioEmail)
        );

        GetItemRequest getRequestReciproco = GetItemRequest.builder()
                .tableName(TABELA)
                .key(chaveReciproco)
                .build();

        GetItemResponse getResponseReciproco = dynamoDb.getItem(getRequestReciproco);

        if (!getResponseReciproco.hasItem()) {
            System.out.printf("Criando reciprocidade %s -> %s%n", contatoEmail, usuarioEmail);

            String createdAt = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

            Map<String, AttributeValue> itemReciproco = Map.of(
                    "usuario_email", AttributeValue.fromS(contatoEmail),
                    "contato_email", AttributeValue.fromS(usuarioEmail),
                    "bloqueado", AttributeValue.fromBool(false),
                    "aceito", AttributeValue.fromBool(true),
                    "createdAt", AttributeValue.fromS(createdAt)
            );

            PutItemRequest putReq = PutItemRequest.builder()
                    .tableName(TABELA)
                    .item(itemReciproco)
                    .build();

            dynamoDb.putItem(putReq);
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}
