package chuyong.springspigot.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SpringSpigotLogger(
    private val colorConverter: ColorConverter,
): Logger by LoggerFactory.getLogger(SpringSpigotLogger::class.java) {
    private val logger = LoggerFactory.getLogger(SpringSpigotLogger::class.java)
    override fun info(msg: String?) {
        val convertedMessage = colorConverter.convert("§f§l[§6SpringSpigot§f§l] $msg")
        logger.info(convertedMessage)
    }

}
