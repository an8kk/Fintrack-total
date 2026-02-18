package com.fintrack.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class SaltEdgeDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaltEdgeConnectRequest {
        @JsonProperty("data")
        private ConnectRequestData data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectRequestData {
        @JsonProperty("customer_id")
        private String customerId;
        private Consent consent;
        private Attempt attempt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Consent {
        private List<String> scopes;
        @JsonProperty("from_date")
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate fromDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attempt {
        @JsonProperty("fetch_scopes")
        private List<String> fetchScopes;
        @JsonProperty("return_to")
        private String returnTo;
        @JsonProperty("notify_url")
        private String notifyUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaltEdgeConnectResponse {
        @JsonProperty("data")
        private ConnectResponseData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectResponseData {
        @JsonProperty("connect_url")
        private String connectUrl;
        @JsonProperty("expires_at")
        private String expiresAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaltEdgeTransactionResponse {
        @JsonProperty("data")
        private List<SaltEdgeTransactionData> data;
        private Meta meta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("next_id")
        private String nextId;
        @JsonProperty("next_page")
        private String nextPage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SaltEdgeTransactionData {
        private String id;
        private String mode;
        private String status;
        @JsonProperty("made_on")
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate madeOn;
        private BigDecimal amount;
        @JsonProperty("currency_code")
        private String currencyCode;
        private String description;
        private String category;
        private Boolean duplicated;
        @JsonProperty("account_id")
        private String accountId;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaltEdgeConnectionResponse {
        @JsonProperty("data")
        private List<ConnectionData> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConnectionData {
        private String id;
        @JsonProperty("customer_id")
        private String customerId;
        @JsonProperty("provider_code")
        private String providerCode;
        @JsonProperty("provider_name")
        private String providerName;
    }
}
