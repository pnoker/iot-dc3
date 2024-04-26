package io.github.ponker.center.ekuiper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author : Zhen
 *
 */
@SpringBootApplication
public class EkuiperApplication {
    private static final Logger log = LoggerFactory.getLogger(EkuiperApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EkuiperApplication.class, args);
        log.info("Ekuiper Application Start...");
    }
}
