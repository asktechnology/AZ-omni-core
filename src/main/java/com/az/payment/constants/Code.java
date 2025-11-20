package com.az.payment.constants;

import lombok.Getter;

@Getter
public enum Code {

    SUCCESS(0, ""),
    CREATED(0, "Created Successfully"),
    NOTFOUND(404, "entity not found"),
    BAD_REQUEST(400, "Bad Request"),
    INVALID(400, "Invalid Request");

    private final int responseCode;
    private final String message;

    Code(int responseCode, String message) {
        this.responseCode = responseCode;
        this.message = message;
    }

    public static Code getByIntCode(int code) {
        for (Code codes : Code.values()) {
            if (code == codes.getResponseCode()) {
                return codes;
            }
        }
        return null;
    }
}
