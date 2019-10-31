package br.com.zapia.catarin.payloads;

public class Notification {
    private String type;
    private Object dado;

    public Notification(String type, Object dado) {
        this.type = type;
        this.dado = dado;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getDado() {
        return dado;
    }

    public void setDado(Object dado) {
        this.dado = dado;
    }
}
