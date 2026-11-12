package vn.doancnpm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.doancnpm.domain.User;
import vn.doancnpm.repository.UserRepository;
import vn.doancnpm.service.AuthService;
import vn.doancnpm.service.JWTUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            if (authService.isTokenBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\": \"Phiên đăng nhập đã kết thúc hoặc token đã bị thu hồi\"}");
                return;
            }
        } catch (Exception ignored) {
        }

        String username = null;
        try {
            username = jwtUtils.extractUsername(token);
        } catch (Exception ignored) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtUtils.getRemainingExpiration(token) > 0) {
                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user != null && user.isStatus()) {
                        String role = user.getRole();
                        String roleAuthority = role != null && role.startsWith("ROLE_") ? role : "ROLE_" + (role != null ? role : "CUSTOMER");
                        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleAuthority));

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                authorities
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        filterChain.doFilter(request, response);
    }
}
