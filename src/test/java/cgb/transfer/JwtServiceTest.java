package cgb.transfer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cgb.transfer.entity.Customer;
import cgb.transfer.entity.Role;
import cgb.transfer.entity.UserCGB;
import cgb.transfer.security.service.JwtService;

@SpringBootTest
public class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private UserCGB testUser;

    @BeforeEach
    void setUp() {
        Role role = new Role("COMPTABLE");
        role.setId(1L);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("GSB");

        testUser = new UserCGB();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(role);
        testUser.setBelongTo(customer);
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void testExtractUsername() {
        String token = jwtService.generateToken(testUser);
        assertEquals("testuser", jwtService.extractUsername(token));
    }

    @Test
    void testExtractRole() {
        String token = jwtService.generateToken(testUser);
        assertEquals("COMPTABLE", jwtService.extractRole(token));
    }

    @Test
    void testExtractCustomerId() {
        String token = jwtService.generateToken(testUser);
        assertEquals(1L, jwtService.extractCustomerId(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.here"));
    }
}
