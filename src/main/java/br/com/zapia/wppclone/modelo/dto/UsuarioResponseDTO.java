package br.com.zapia.wppclone.modelo.dto;

import java.util.List;

public class UsuarioResponseDTO extends UsuarioBasicResponseDTO {

    private List<UsuarioBasicResponseDTO> usuariosFilhos;

    public List<UsuarioBasicResponseDTO> getUsuariosFilhos() {
        return usuariosFilhos;
    }

    public void setUsuariosFilhos(List<UsuarioBasicResponseDTO> usuariosFilhos) {
        this.usuariosFilhos = usuariosFilhos;
    }
}
