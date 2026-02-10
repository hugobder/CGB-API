package cgb.transfer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/test")
public class ExempleTestController {
	
	@GetMapping("/{id}")
	public String obtenirTache(@PathVariable int id) {
		
		return "Recu : " + id ; // Si application properties avec spring.thymeleaf.prefix=classpath:/vues/

	}
	
	@GetMapping("/")
	public String testVide() {
		
		return "Racine sous test "  ; // Si application properties avec spring.thymeleaf.prefix=classpath:/vues/

	}
}
