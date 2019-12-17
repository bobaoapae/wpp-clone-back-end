package br.com.zapia.wppclone.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WsMessage {

    private String tag;
    private Object obj;

    public WsMessage(String tag, Object obj) {
        this.tag = tag;
        this.obj = obj;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    @Override
    public String toString() {
        try {
            String dado;
            if (obj instanceof String) {
                dado = (String) obj;
            } else {
                dado = new ObjectMapper().writeValueAsString(obj);
            }
            return tag.toLowerCase() + "," + dado;
        } catch (JsonProcessingException e) {
            return "error, " + e.toString();
        }
    }
}
