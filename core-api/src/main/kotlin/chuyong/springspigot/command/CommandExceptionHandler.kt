package chuyong.springspigot.command

interface CommandExceptionHandler {
    fun handleCommandMappingException(ex: Throwable)
}
