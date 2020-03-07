package br.com.zapia.wppclone.payloads;

import java.util.Map;

public class AddCustomPropertyRequest {

    private String id;
    private Map.Entry<String, String> value;
    private Type type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map.Entry<String, String> getValue() {
        return value;
    }

    public void setValue(Map.Entry<String, String> value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {
        CHAT,
        MSG
    }
}
