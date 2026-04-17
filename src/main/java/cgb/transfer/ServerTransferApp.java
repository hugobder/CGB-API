package cgb.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ServerTransferApp {

	public static void main(String[] args) {
		SpringApplication.run(ServerTransferApp.class, args);
	}
}

