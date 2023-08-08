package chuyong.springspigot.command.annotation

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
annotation class CommandMapping(
    val value: String = "",
    val child: String = "",
    val usage: String = "",
    val prefix: String = "",
    val perm: String = "",
    val error: String = "명령어 사용법이 올바르지 않습니다",
    val noPerm: String = "이 명령어를 실행할 권한이 없습니다",
    val noConsole: String = "콘솔에서 사용할 수 없습니다",
    val aliases: Array<String> = [],
    val suggestion: Array<String> = [],
    val minArgs: Int = 0,
    val maxArgs: Int = 100,
    val defaultSuggestion: Boolean = false,
    val op: Boolean = false,
    val console: Boolean = false,
)
