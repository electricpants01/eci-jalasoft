package eci.technician.models;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class LoginRegisterModel {
    @SerializedName("Email")
    private String email;
    @SerializedName("Password")
    private String password;
    @SerializedName("EAutomateId")
    private String eAutomateId;
    @SerializedName("CompanyId")
    private UUID companyId;

    public LoginRegisterModel(String email, String password, String eAutomateId) {
        this.email = email;
        this.password = password;
        this.eAutomateId = eAutomateId;
        this.companyId = UUID.fromString("b143d6dc-3dec-e511-b041-8c89a5feb541");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String geteAutomateId() {
        return eAutomateId;
    }

    public void seteAutomateId(String eAutomateId) {
        this.eAutomateId = eAutomateId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
}
