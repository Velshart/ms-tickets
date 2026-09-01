package me.mmtr.userservice.service;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTests {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    public void shouldFindUserByUsername() {
        String username = "testUser";
        String password = "password";
        String email = "test@example.com";
        Role role = Role.USER;
        User user = new User(username, email, password, role);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        UserDetails loadedUser =
                customUserDetailsService.loadUserByUsername(username);

        assertEquals(username, loadedUser.getUsername());
        assertEquals(password, loadedUser.getPassword());
        assertTrue(loadedUser.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_" + Role.USER)
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        String username = "testUser";
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(username)
        );
    }
}
