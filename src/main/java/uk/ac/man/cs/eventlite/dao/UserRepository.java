package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import uk.ac.man.cs.eventlite.entities.User;

public interface UserRepository extends CrudRepository<User, Long>{
    User findByUsername(String username);
}