package edu.cit.redoble.features.notification.dto;

public class DeviceRegisterRequest {
    private String token;
    private String platform;

    public DeviceRegisterRequest() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
