package chuyong.springspigot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling


@EnableScheduling
@SpringBootApplication(scanBasePackages = ["chuyong.springspigot"])
class SpringSpigotApplication
