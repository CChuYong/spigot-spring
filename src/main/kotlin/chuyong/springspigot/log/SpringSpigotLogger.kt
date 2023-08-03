package chuyong.springspigot.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SpringSpigotLogger: Logger by LoggerFactory.getLogger(SpringSpigotLogger::class.java) {
    private val logger = LoggerFactory.getLogger(SpringSpigotLogger::class.java)
    override fun info(msg: String?) {
        logger.info("§f§l[§6SpringSpigot§f§l] $msg")
    }
}
