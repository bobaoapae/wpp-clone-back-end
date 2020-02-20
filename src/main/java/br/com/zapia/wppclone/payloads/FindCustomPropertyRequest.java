package br.com.zapia.wppclone.payloads;

public class FindCustomPropertyRequest {

    private Type type;
    private String id;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public enum Type {
        CHAT,
        MSG
    }

}
