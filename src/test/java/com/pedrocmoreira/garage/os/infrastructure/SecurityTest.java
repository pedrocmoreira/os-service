package com.pedrocmoreira.garage.os.infrastructure;

import com.pedrocmoreira.garage.os.domain.model.User;
import com.pedrocmoreira.garage.os.infrastructure.persistence.UserJpaRepository;
import com.pedrocmoreira.garage.os.infrastructure.security.JwtAuthenticationFilter;
import com.pedrocmoreira.garage.os.infrastructure.security.JwtTokenProvider;
import com.pedrocmoreira.garage.os.infrastructure.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityTest {
    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres!!";
    private final UserJpaRepository userRepository = mock(UserJpaRepository.class);
    private final UserDetailsServiceImpl userDetailsService = new UserDetailsServiceImpl(userRepository);
    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private User admin(boolean active) {
        return User.builder().username("admin").password("x").name("Admin").userType(User.UserType.ADMIN).active(active).build();
    }

    @Test
    void shouldGenerateAndValidateToken() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin(true)));
        UserDetails user = userDetailsService.loadUserByUsername("admin");
        String token = provider.generateToken(user);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.extractUsername(token)).isEqualTo("admin");
        assertThat(provider.getExpirationMs()).isEqualTo(60_000);
        assertThat(provider.validateToken("invalido")).isFalse();
        assertThat(new JwtTokenProvider("outro-segredo-com-mais-de-32-caracteres-xx", 1000).validateToken(token)).isFalse();
    }

    @Test
    void userDetailsShouldRejectUnknownOrInactiveUsers() {
        when(userRepository.findByUsername("x")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin(false)));
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("x")).isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("admin")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void filterShouldAuthenticateValidBearerToken() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin(true)));
        String token = provider.generateToken(userDetailsService.loadUserByUsername("admin"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_ADMIN");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void filterShouldIgnoreMissingOrInvalidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc");
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
