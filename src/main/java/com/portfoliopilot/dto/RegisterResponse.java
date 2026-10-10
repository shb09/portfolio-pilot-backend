package com.portfoliopilot.dto;

/**
 * Registration never logs the user in. Frontend shows the message and,
 * in local dev mode only, may receive a dev token for testing.
 */
public record RegisterResponse(String message, boolean verificationPending, String devToken) {

    public static RegisterResponse pending(String message) {
        return new RegisterResponse(message, true, null);
    }

    public static RegisterResponse pendingDev(String message, String devToken) {
        return new RegisterResponse(message, true, devToken);
    }
}
