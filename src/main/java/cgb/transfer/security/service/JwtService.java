package cgb.transfer.security.service;

import org.springframework.stereotype.Service;
/* S2
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
 */

@Service
public class JwtService {

	private static final String FIXED_TOKEN = "f51a24ca-9486-4f77-97dd-51dad1467a9e";

	public String generateToken(String username) {
		return FIXED_TOKEN;
	}


	/* S2
       private static final String SECRET_KEY = "secret";
    private static final String FIXED_SUBJECT = "user123";

    public String generateToken() {
        return Jwts.builder()
                .setSubject(FIXED_SUBJECT)
                .setIssuedAt(new Date(0)) // Date fixe
                .setExpiration(new Date(Long.MAX_VALUE)) // Expiration très éloignée
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
	 */

	//S3
	//Dans app.properties jwt.fixed.token=f51a24ca-9486-4f77-97dd-51dad1467a9e
	/*
	 * import org.springframework.beans.factory.annotation.Value; import
	 * org.springframework.stereotype.Service;
	 * 
	 * @Service public class JwtService {
	 * 
	 * @Value("${jwt.fixed.token}") private String fixedToken;
	 * 
	 * public String generateToken() { return fixedToken; } }
	 */


}