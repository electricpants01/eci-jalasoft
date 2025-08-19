package eci.technician.models;

public class LastUserModel {
    private String account;
    private String username;
    private String password;
    private boolean save;

    public LastUserModel(String account, String username, String password, boolean save) {
        this.account = account;
        this.username = username;
        this.password = password;
        this.save = save;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public boolean isSave() {
        return save;
    }

    public void setSave(boolean save) {
        this.save = save;
    }
}
