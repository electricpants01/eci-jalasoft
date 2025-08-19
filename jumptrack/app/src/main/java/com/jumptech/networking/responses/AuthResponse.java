package com.jumptech.networking.responses;

public class AuthResponse {

    private String authorization;
    private boolean isEulaPrompt;
    private ConfigResponse config;

    public String getAuthorization() {
        return authorization;
    }

    public boolean isEulaPrompt() {
        return isEulaPrompt;
    }

    public ConfigResponse getConfig() {
        return config;
    }
}
