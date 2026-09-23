package com.safework.safework.security;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.safework.safework.model.Usuario;
import com.safework.safework.repository.UsuarioRepository;



@Service
public class CustomUserDetailsService 
        implements UserDetailsService {



    private final UsuarioRepository usuarioRepository;



    public CustomUserDetailsService(
            UsuarioRepository usuarioRepository){

        this.usuarioRepository = usuarioRepository;

    }



    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {



        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "Usuario no encontrado"
                    )
                );



        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRol())
                .disabled(!"ACTIVO".equals(usuario.getEstado()))
                .build();

    }

}
