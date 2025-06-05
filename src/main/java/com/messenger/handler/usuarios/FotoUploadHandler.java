package com.messenger.handler.usuarios;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.messenger.shared.auth.TokenValidator;
import org.apache.commons.fileupload.MultipartStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class FotoUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final TokenValidator tokenValidator = new TokenValidator();

    private static final S3Client s3 = S3Client.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();

    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String BUCKET = "messenger-fotos";
    private static final String TABELA = "usuarios";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        String authHeader = Optional.ofNullable(request.getHeaders()).map(h -> h.get("Authorization")).orElse(null);

        if (!tokenValidator.isTokenValid(authHeader)) {
            return buildResponse(401, "Token inválido ou ausente");
        }

        String email = tokenValidator.getEmailFromToken(authHeader);

        try {
            String contentType = request.getHeaders().get("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                return buildResponse(400, "Content-Type inválido. Esperado multipart/form-data");
            }

            String boundary = contentType.split("boundary=")[1];

            InputStream inputStream;
            if (Boolean.TRUE.equals(request.getIsBase64Encoded())) {
                inputStream = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(request.getBody()));
            } else {
                inputStream = new ByteArrayInputStream(request.getBody().getBytes(StandardCharsets.ISO_8859_1));
            }

            MultipartStream multipartStream = new MultipartStream(inputStream, boundary.getBytes(StandardCharsets.UTF_8), 1024, null);

            boolean nextPart = multipartStream.skipPreamble();
            byte[] fileBytes = null;
            String filename = "perfil/" + email + ".jpg";

            while (nextPart) {
                String headers = multipartStream.readHeaders();
                if (headers.contains("filename=\"")) {
                    // Lê os bytes do arquivo usando OutputStream
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    multipartStream.readBodyData(output);
                    fileBytes = output.toByteArray();
                    break;
                } else {
                    multipartStream.discardBodyData();
                }
                nextPart = multipartStream.readBoundary();
            }

            if (fileBytes == null) {
                return buildResponse(400, "Arquivo não encontrado no body da requisição");
            }

            // Salva no S3
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(BUCKET)
                    .key(filename)
                    .contentType("image/jpeg")
                    .build();

            s3.putObject(putReq, RequestBody.fromBytes(fileBytes));

            // Atualiza DynamoDB
            UpdateItemRequest updateReq = UpdateItemRequest.builder()
                    .tableName(TABELA)
                    .key(Map.of("id", AttributeValue.fromS(email)))
                    .updateExpression("SET fotoPerfil = :foto")
                    .expressionAttributeValues(Map.of(":foto", AttributeValue.fromS(filename)))
                    .build();

            dynamoDb.updateItem(updateReq);

            return buildResponse(200, "Foto de perfil atualizada com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "Erro ao processar upload: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}
