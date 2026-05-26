package edu.cit.redoble.features.auth.dto;

public class GoogleSignInRequest {
    private String idToken;

    public GoogleSignInRequest() {}

    public GoogleSignInRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
