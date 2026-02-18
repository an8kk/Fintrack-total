package com.fintrack.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class GeminiDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        @JsonProperty("contents")
        private List<Content> contents;

        @JsonProperty("systemInstruction")
        private Content systemInstruction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        @JsonProperty("role")
        private String role;
        @JsonProperty("parts")
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        @JsonProperty("text")
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiResponse {
        @JsonProperty("candidates")
        private List<Candidate> candidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        @JsonProperty("content")
        private Content content;
    }
}
