package com.duoc.guias.dto;

public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private TransportistaDTO transportista;

    public LoginResponse() { }

    public LoginResponse(String token, long expiresIn, TransportistaDTO transportista) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.transportista = transportista;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public TransportistaDTO getTransportista() { return transportista; }
    public void setTransportista(TransportistaDTO transportista) { this.transportista = transportista; }
}
