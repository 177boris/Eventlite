package uk.ac.man.cs.eventlite.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.man.cs.eventlite.entities.Permission;
import uk.ac.man.cs.eventlite.entities.Role;
import uk.ac.man.cs.eventlite.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service("userDetailsService")
@Transactional
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        User user = userRepository.findByUsername(username);
        if(user == null)
            throw new UsernameNotFoundException("Unrecognised credentials");
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
                user.isEnabled(), true, true, true,
                getAuthorities(user.getRoles()));
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Collection<Role> roles){
        return getGrantedAuthorities(getPermissions(roles));
    }

    private List<String> getPermissions(Collection<Role> roles){
        List<String> permissions = new ArrayList<>();
        List<Permission> collection = new ArrayList<>();
        for(Role role : roles) {
            collection.addAll(role.getPermissions());
            permissions.add(role.getName());
        }
        for(Permission permission : collection)
            permissions.add(permission.getName());
        return permissions;
    }

    private List<GrantedAuthority> getGrantedAuthorities(List<String> permissions){
        List<GrantedAuthority> authorities = new ArrayList<>();
        for(String permission : permissions)
            authorities.add(new SimpleGrantedAuthority(permission));
        return authorities;
    }
}
