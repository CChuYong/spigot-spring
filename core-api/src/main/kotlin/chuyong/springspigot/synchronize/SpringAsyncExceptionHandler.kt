package chuyong.springspigot.synchronize

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Component
class SpringAsyncExceptionHandler : AsyncUncaughtExceptionHandler {
    override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any?) {
        ex.printStackTrace()
    }
}
