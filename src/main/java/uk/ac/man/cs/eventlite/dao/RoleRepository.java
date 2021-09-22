package uk.ac.man.cs.eventlite.dao;


import org.springframework.data.repository.CrudRepository;
import uk.ac.man.cs.eventlite.entities.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {
    Role findByName(String name);
}
