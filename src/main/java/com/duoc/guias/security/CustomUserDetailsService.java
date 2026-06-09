package com.duoc.guias.security;

import com.duoc.guias.model.Transportista;
import com.duoc.guias.repository.TransportistaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final TransportistaRepository transportistaRepository;

    public CustomUserDetailsService(TransportistaRepository transportistaRepository) {
        this.transportistaRepository = transportistaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Transportista t = transportistaRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Transportista not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                t.getUsername(),
                t.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + t.getRole()))
        );
    }
}
