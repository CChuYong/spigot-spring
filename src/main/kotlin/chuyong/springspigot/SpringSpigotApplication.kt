package chuyong.springspigot

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling


@EnableScheduling
@ComponentScan(basePackages = ["chuyong.springspigot"])
@Configuration
class SpringSpigotApplication
