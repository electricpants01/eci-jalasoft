package com.jumptech.util;

public class TestUser {
    private String username;
    private String password;
    private boolean valid;

    public TestUser(String username, String password, boolean valid) {
        this.username = username;
        this.password = password;
        this.valid = valid;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isValid() {
        return valid;
    }
}
