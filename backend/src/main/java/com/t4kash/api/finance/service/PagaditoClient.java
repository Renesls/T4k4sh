package com.t4kash.api.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.exception.PaymentProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PagaditoClient {
    private static final String SUCCESS_CONNECT = "PG1001";
    private static final String SUCCESS_TRANSACTION = "PG1002";
    private static final String SUCCESS_STATUS = "PG1003";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String endpoint;
    private final String uid;
    private final String wsk;

    public PagaditoClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.pagadito.enabled:false}") boolean enabled,
            @Value("${app.pagadito.endpoint:https://sandbox.pagadito.com/comercios/wspg/charges.php}") String endpoint,
            @Value("${app.pagadito.uid:}") String uid,
            @Value("${app.pagadito.wsk:}") String wsk
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.uid = uid;
        this.wsk = wsk;
    }

    public Checkout checkout(
            String commerceReference,
            BigDecimal amount,
            String description,
            String taskUrl
    ) {
        requireConfiguration();
        String connectionToken = connect();
        String details = writeJson(List.of(Map.of(
                "quantity", 1,
                "description", description,
                "price", amount,
                "url_product", taskUrl
        )));
        String customParams = writeJson(Map.of("referencia_t4kash", commerceReference));
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("token", connectionToken);
        parameters.put("ern", commerceReference);
        parameters.put("amount", amount.toPlainString());
        parameters.put("details", details);
        parameters.put("format_return", "json");
        parameters.put("currency", "NIO");
        parameters.put("custom_params", customParams);

        JsonNode response = call("exec_trans", parameters);
        requireCode(response, SUCCESS_TRANSACTION);
        String checkoutUrl = response.path("value").asText();
        if (checkoutUrl.isBlank()) {
            throw new PaymentProviderException("Pagadito no devolvio la direccion de pago.");
        }
        return new Checkout(checkoutUrl, extractTransactionToken(checkoutUrl));
    }

    public TransactionStatus getStatus(String transactionToken) {
        requireConfiguration();
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("token", connect());
        parameters.put("token_trans", transactionToken);
        parameters.put("format_return", "json");
        JsonNode response = call("get_status", parameters);
        requireCode(response, SUCCESS_STATUS);
        JsonNode value = response.path("value");
        if (value.isTextual()) {
            try {
                value = objectMapper.readTree(value.asText());
            } catch (Exception exception) {
                throw new PaymentProviderException("Pagadito devolvio un estado no reconocido.", exception);
            }
        }
        return new TransactionStatus(
                value.path("status").asText().toUpperCase(Locale.ROOT),
                value.path("reference").asText(null),
                value.path("date_trans").asText(null)
        );
    }

    private String connect() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("uid", uid);
        parameters.put("wsk", wsk);
        parameters.put("format_return", "json");
        JsonNode response = call("connect", parameters);
        requireCode(response, SUCCESS_CONNECT);
        return response.path("value").asText();
    }

    private JsonNode call(String operation, Map<String, String> parameters) {
        try {
            String responseXml = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.TEXT_XML)
                    .header("SOAPAction", "urn:ws#" + operation)
                    .body(soapEnvelope(operation, parameters))
                    .retrieve()
                    .body(String.class);
            String encodedJson = extractSoapReturn(responseXml);
            return objectMapper.readTree(encodedJson);
        } catch (PaymentProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PaymentProviderException(
                    "No fue posible comunicarse con Pagadito Sandbox.",
                    exception
            );
        }
    }

    private String soapEnvelope(String operation, Map<String, String> parameters) {
        StringBuilder body = new StringBuilder();
        parameters.forEach((name, value) -> body.append('<').append(name)
                .append(" xsi:type=\"xsd:string\">")
                .append(escapeXml(value))
                .append("</").append(name).append('>'));
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                    xmlns:urn="urn:wspg">
                  <soapenv:Body>
                    <urn:%s soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                      %s
                    </urn:%s>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(operation, body, operation);
    }

    private String extractSoapReturn(String responseXml) throws Exception {
        if (responseXml == null || responseXml.isBlank()) {
            throw new PaymentProviderException("Pagadito devolvio una respuesta vacia.");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(responseXml.getBytes(StandardCharsets.UTF_8))
        );
        var returns = document.getElementsByTagNameNS("*", "return");
        if (returns.getLength() == 0) {
            throw new PaymentProviderException("La respuesta SOAP de Pagadito no contiene datos.");
        }
        return returns.item(0).getTextContent();
    }

    private String extractTransactionToken(String checkoutUrl) {
        try {
            String query = URI.create(checkoutUrl).getRawQuery();
            if (query == null) {
                return null;
            }
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (("token".equalsIgnoreCase(key) || "token_trans".equalsIgnoreCase(key))
                        && parts.length == 2) {
                    return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void requireCode(JsonNode response, String expectedCode) {
        if (!expectedCode.equals(response.path("code").asText())) {
            String message = response.path("message").asText("Respuesta no reconocida.");
            throw new PaymentProviderException("Pagadito rechazo la operacion: " + message);
        }
    }

    private void requireConfiguration() {
        if (!enabled || uid.isBlank() || wsk.isBlank()) {
            throw new PaymentProviderException(
                    "Pagadito Sandbox aun no esta configurado en el servidor."
            );
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new PaymentProviderException("No se pudo preparar el detalle del pago.", exception);
        }
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public record Checkout(String url, String transactionToken) { }
    public record TransactionStatus(String status, String reference, String transactionDate) { }
}
