package br.com.zapia.wppclone.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import org.springframework.security.core.GrantedAuthority;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Entity
public class Permissao extends Entidade implements GrantedAuthority {

    @NotBlank
    @Column(unique = true, nullable = false)
    private String permissao;
    @OneToMany(mappedBy = "permissao", cascade = CascadeType.ALL)
    private List<Usuario> usuarios;

    public Permissao(String permissao) {
        this.permissao = permissao;
    }

    public Permissao() {
    }

    @Override
    public String getAuthority() {
        return permissao;
    }

    public String getPermissao() {
        return permissao;
    }

    public void setPermissao(String permissao) {
        this.permissao = permissao;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }
}
