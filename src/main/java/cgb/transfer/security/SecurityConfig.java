package cgb.transfer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain; 


/*
 * 
 * Class qui peut surcharger la configuration de sécurité par défaut incluse 
 * dès que la dépendance spring-boot-starter-security est incluse.
 * Si vous ne souhaitez pas que Spring Security s'applique automatiquement, 
 * vous pouvez exclure spring-boot-starter-security de vos dépendances 
 * ou fournir une configuration qui désactive la sécurité 
 * (ce qui n'est généralement pas recommandé pour les applications de production).
 * Avec la config de sécurité par défaut, un password est automatiquement généré par spring boot 
 * pour pouvoir accéder à l'API avec le header suivant
 * 
 * Headers :
 * 	Content-Type: application/json
 * 	Authorization: Basic <votre_token_d_authentification>
 * 
 * avec : comme jeton, la concaténation de l'utilisateur et du mot de passe 
 * sous la forme user:password, le tout encodez en Base64.
 * 
 * Attention ce mécanisme de base n'inclus par de chiffrement http. 
 * 
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		.csrf(csrf -> csrf.disable()) // Désactive le CSRF
		.authorizeHttpRequests(
				(requests) -> requests.anyRequest().authenticated() //authorise les requêtes identifiées
				)
		.httpBasic(Customizer.withDefaults())
		.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())); //Pour H2 en même frame
		return http.build();
	}

}
