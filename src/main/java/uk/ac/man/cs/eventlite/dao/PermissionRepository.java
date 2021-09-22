package uk.ac.man.cs.eventlite.dao;


import org.springframework.data.repository.CrudRepository;
import uk.ac.man.cs.eventlite.entities.Permission;

public interface PermissionRepository extends CrudRepository<Permission, Long> {

    Permission findByName(String name);
}
