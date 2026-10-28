package com.safework.safework.security;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
public class CustomUserDetailsService 
        implements UserDetailsService {



    private final UsuarioAccesoRepository usuarioRepository;



    public CustomUserDetailsService(
            UsuarioAccesoRepository usuarioRepository){

        this.usuarioRepository = usuarioRepository;

    }



    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {



        var usuario = usuarioRepository
                .buscar(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "Usuario no encontrado"
                    )
                );



        return new EmpresaPrincipal(usuario.id(), usuario.username(), usuario.password(), usuario.rol(),
                usuario.habilitado(), usuario.empresaId(), usuario.empresaNombre());

    }

}
