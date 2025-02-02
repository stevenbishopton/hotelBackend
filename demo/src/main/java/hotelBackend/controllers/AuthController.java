package hotelBackend.controllers;

import hotelBackend.Security.CurrentUser;
import hotelBackend.Security.JwtTokenProvider;
import hotelBackend.dtos.LoginRequest;
import hotelBackend.dtos.LoginResponse;
import hotelBackend.dtos.UserResponse;
import hotelBackend.response.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    //dependencies
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    //login
    @PostMapping("/login")
    public ResponseEntity<?> login (@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new LoginResponse(jwt, userDetails.getUsername(), roles));
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);  // Add logging
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed","Invalid username or password"));
        }
    }

    //crosscheck if im authenticated
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CurrentUser UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("No authenticated user found");  // Add logging
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", "No authenticated user found"));
        }
        log.info("Current user request for: {}", userDetails.getUsername());  // Add logging
        return ResponseEntity.ok(new UserResponse(userDetails));
    }
}