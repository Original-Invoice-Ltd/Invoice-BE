package invoice.utiils;


import invoice.dtos.request.InitializePaymentRequest;
import invoice.dtos.request.TransferRequest;
import invoice.dtos.response.*;
import invoice.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackApiClient {
    private final RestTemplate externalApiRestTemplate;

    @Value("${paystack.api.url}")
    private String paystackApiBaseUrl;

    @Value("${paystack.api.secret.key}")
    private String secretKey;

    public InitializePaymentResponse initializePayment(InitializePaymentRequest request) {
        String url = paystackApiBaseUrl + "/transaction/initialize";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);

        String reference = generateUniqueReference();
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", request.getEmail());
        payload.put("amount", request.getAmount() * 100); // Paystack expects amount in kobo
        payload.put("reference", reference);
        payload.put("currency", request.getCurrency() != null ? request.getCurrency() : "NGN");
        payload.put("callback_url", request.getCallbackUrl());
        payload.put("channels", new String[]{"card", "bank"});

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            // Use ParameterizedTypeReference to get Map response directly
            ResponseEntity<Map<String, Object>> response = externalApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.info("Response :{}", response.getBody());

            // Convert the Map response to InitializePaymentResponse object
            return convertMapToInitializePaymentResponse(response.getBody());

        } catch (RestClientException e) {
            throw new BusinessException("Failed to initialize payment with Paystack: " + e.getMessage());
        }
    }

    private InitializePaymentResponse convertMapToInitializePaymentResponse(Map<String, Object> responseMap) {
        if (responseMap == null) {
            return null;
        }

        InitializePaymentResponse response = new InitializePaymentResponse();
        // Set top-level fields
        response.setStatus(String.valueOf(responseMap.get("status")));
        response.setMessage(String.valueOf(responseMap.get("message")));

        if (responseMap.containsKey("execTime")) {
            response.setExecTime(Double.valueOf(String.valueOf(responseMap.get("execTime"))));
        }

        if (responseMap.containsKey("error")) {
            response.setError((Object[]) responseMap.get("error"));
        }

        if (responseMap.containsKey("errorMessage")) {
            response.setErrorMessage(String.valueOf(responseMap.get("errorMessage")));
        }

        // Handle nested data object
        if (responseMap.containsKey("data") && responseMap.get("data") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");

            InitializePaymentResponse.PaymentData paymentData = new InitializePaymentResponse.PaymentData();

            if (dataMap.containsKey("authorization_url")) {
                paymentData.setAuthorizationUrl(String.valueOf(dataMap.get("authorization_url")));
            }
            if (dataMap.containsKey("access_code")) {
                paymentData.setAccessCode(String.valueOf(dataMap.get("access_code")));
            }
            if (dataMap.containsKey("reference")) {
                paymentData.setReference(String.valueOf(dataMap.get("reference")));
            }

            response.setData(paymentData);
        }

        return response;
    }

    public VerifyPaymentResponse verifyPayment(String reference) {
        String url = paystackApiBaseUrl + "/transaction/verify/" + reference;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<VerifyPaymentResponse> response = externalApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    VerifyPaymentResponse.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            throw new BusinessException("Failed to verify payment with Paystack: " + e.getMessage());
        }
    }

    public TransferResponse initiateTransfer(String recipientCode, double amount, String reason, String reference) {
        String url = paystackApiBaseUrl + "/transfer";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("source", "balance");
        payload.put("amount", amount * 100);
        payload.put("recipient", recipientCode);
        payload.put("reason", reason);
        payload.put("reference", reference);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map<String, Object>> response = externalApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return convertMapToTransferResponse(response.getBody());
        } catch (RestClientException e) {
            throw new BusinessException("Failed to initiate transfer with Paystack: " + e.getMessage());
        }
    }

    public VerifyPaymentResponse verifyTransfer(String reference) {
        String url = paystackApiBaseUrl + "/transfer/verify/" + reference;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<VerifyPaymentResponse> response = externalApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    VerifyPaymentResponse.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            throw new BusinessException("Failed to verify payment with Paystack: " + e.getMessage());
        }
    }

    private TransferResponse convertMapToTransferResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }

        TransferResponse response = new TransferResponse();
        // Set top-level fields
        response.setStatus(String.valueOf(body.get("status")));
        response.setMessage(String.valueOf(body.get("message")));

        // Handle nested data object
        if (body.containsKey("data") && body.get("data") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");

            TransferResponse.Data data = new TransferResponse.Data();

            data.setReference(String.valueOf(dataMap.get("reference")));
            data.setAmount(Double.parseDouble(String.valueOf(dataMap.get("amount"))));
            data.setRecipient(String.valueOf(dataMap.get("recipient")));
            response.setData(data);
        }
        return response;
    }

    private String generateUniqueReference() {
        return "PS-" + UUID.randomUUID().toString().replaceAll("[^a-z0-9_-]", "").substring(0, 16);
    }

    public BankListResponse getBanks() {
        String url = paystackApiBaseUrl + "/bank?country=nigeria";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<BankListResponse> response = externalApiRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    BankListResponse.class
            );

            log.info("Bank list response: {}", response.getBody());
            return response.getBody();

        } catch (RestClientException e) {
            throw new BusinessException("Failed to fetch banks from Paystack: " + e.getMessage());
        }
    }

    public AccountValidationResponse validateAccountNumber(String accountNumber, String bankCode) {
        String url = paystackApiBaseUrl + "/bank/resolve";

        // Build query parameters
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("account_number", accountNumber)
                .queryParam("bank_code", bankCode);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<AccountValidationResponse> response = externalApiRestTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    AccountValidationResponse.class
            );

            log.info("Account validation response: {}", response.getBody());
            return response.getBody();

        } catch (RestClientException e) {
            throw new BusinessException("Failed to validate account number with Paystack: " + e.getMessage());
        }
    }

    public RecipientResponse createRecipient(@Valid TransferRequest request) {
        String url = paystackApiBaseUrl + "/transferrecipient";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "nuban");
        payload.put("name", request.getName());
        payload.put("account_number", request.getAccountNumber());
        payload.put("bank_code", request.getBankCode());
        payload.put("currency", "NGN");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            // Use ParameterizedTypeReference to get Map response directly
            ResponseEntity<Map<String, Object>> response = externalApiRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            // Convert the Map response to InitializePaymentResponse object
            return convertMapToRecipientResponse(response.getBody());

        } catch (RestClientException e) {
            throw new BusinessException("Failed to create recipient with Paystack: " + e.getMessage());
        }
    }

    private RecipientResponse convertMapToRecipientResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }

        RecipientResponse response = new RecipientResponse();
        // Set top-level fields
        response.setStatus(String.valueOf(body.get("status")));
        response.setMessage(String.valueOf(body.get("message")));

        // Handle nested data object
        if (body.containsKey("data") && body.get("data") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) body.get("data");

            RecipientResponse.Data data = new RecipientResponse.Data();

            data.setRecipientCode(String.valueOf(dataMap.get("recipient_code")));
            response.setData(data);
        }
        return response;
    }
}