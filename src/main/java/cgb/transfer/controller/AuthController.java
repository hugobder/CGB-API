package cgb.transfer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import cgb.transfer.entity.UserCGB;
import cgb.transfer.repository.UserCGBRepository;
import cgb.transfer.security.service.JwtService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserCGBRepository userCGBRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<UserCGB> userOpt = userCGBRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Identifiants invalides"));
        }

        UserCGB user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Identifiants invalides"));
        }

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername(), "role", user.getRole().getName()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserCGB newUser) {
        if (userCGBRepository.findByUsername(newUser.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Utilisateur deja existant"));
        }
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        userCGBRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Utilisateur cree"));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserCGB updatedUser) {
        Optional<UserCGB> userOpt = userCGBRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Utilisateur introuvable"));
        }
        UserCGB user = userOpt.get();
        if (updatedUser.getUsername() != null) user.setUsername(updatedUser.getUsername());
        if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null) user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        if (updatedUser.getRole() != null) user.setRole(updatedUser.getRole());
        if (updatedUser.getBelongTo() != null) user.setBelongTo(updatedUser.getBelongTo());
        userCGBRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Utilisateur mis a jour"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userCGBRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Utilisateur introuvable"));
        }
        userCGBRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprime"));
    }
}
