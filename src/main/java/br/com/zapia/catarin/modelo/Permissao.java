package br.com.zapia.catarin.modelo;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uuid")
@Entity
public class Permissao extends Entidade implements GrantedAuthority {

    @Column(unique = true)
    private String permissao;
    @JsonBackReference
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
