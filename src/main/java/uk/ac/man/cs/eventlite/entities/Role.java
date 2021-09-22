package uk.ac.man.cs.eventlite.entities;

import javax.persistence.*;
import java.util.Collection;

@Entity
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String name;
    @ManyToMany(mappedBy = "roles")
    private Collection<User> users;

    @ManyToMany
    Collection<Permission> permissions;

    public Role(){
        super();
    }
    public Role(String name){
        super();
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public Collection<Permission> getPermissions(){
        return permissions;
    }

    public void setPermissions(Collection<Permission> permissions){
        this.permissions = permissions;
    }
}
