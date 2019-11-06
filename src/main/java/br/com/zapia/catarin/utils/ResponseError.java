package br.com.zapia.catarin.utils;

import java.io.Serializable;

public class ResponseError implements Serializable {
    private String msg;

    public ResponseError(Exception e) {
        Throwable cause = e;
        do {
            cause = cause.getCause();
        } while (cause.getCause() != null);
        msg = cause.getLocalizedMessage();
        if (msg.lines().count() > 1) {
            msg = msg.lines().skip(1).findFirst().get();
        }
    }

    public ResponseError(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return msg;
    }
}
