package uk.ac.man.cs.eventlite.entities;

import javax.persistence.*;
import java.util.Collection;

@Entity
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String name;
    @ManyToMany(mappedBy = "permissions")
    Collection<Role> roles;

    public Permission(){
        super();
    }

    public Permission(String name){
        super();
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
