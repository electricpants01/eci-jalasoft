package com.jumptech.tracklib.data;

import com.google.gson.GsonBuilder;

public class Login {

    String _server;
    String _username;

    private class LoginInfo {
        String s;
        String u;
    }

    private Login(String server, String username){
        _server = server;
        _username = username;
    }

    public String getServer() {
        return _server;
    }

    public String getUsername() {
        return _username;
    }

    public static Login parse(String str) {
        LoginInfo loginInfo = new GsonBuilder().create().fromJson(str, LoginInfo.class);

        return new Login(loginInfo.s, loginInfo.u);
    }

}