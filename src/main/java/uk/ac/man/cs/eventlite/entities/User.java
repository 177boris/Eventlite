package uk.ac.man.cs.eventlite.entities;

import javax.persistence.*;
import java.util.Collection;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String username;
    private String password;
    private boolean enabled;
    @ManyToMany
    private Collection<Role> roles;

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public boolean isEnabled(){
        return enabled;
    }

    public Collection<Role> getRoles(){
        return roles;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void setRoles(Collection<Role> roles){
        this.roles = roles;
    }

    public void setEnabled(boolean isEnabled){
        this.enabled = isEnabled;
    }
}
