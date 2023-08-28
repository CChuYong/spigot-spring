package chuyong.springspigot.external.dependency

import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.GlobalContext.getKoinApplicationOrNull
import org.koin.core.qualifier.StringQualifier
import org.koin.core.scope.Scope
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component("koin-provider")
@ConditionalOnClass(KoinApplication::class, Scope::class)
class KoinDependencyProvider : ExternalDependencyProvider {
    @OptIn(KoinInternalApi::class)
    private val scope = run {
        val koinApplication = getKoinApplicationOrNull()
        koinApplication?.koin?.scopeRegistry?.rootScope
    }

    override fun <T : Any> get(clazz: Class<T>): T {
        val mainScope = scope ?: throw RuntimeException("Koin Not Initialized")
        val kotlinClazz: KClass<T> = clazz.kotlin
        return mainScope.get(kotlinClazz, null, null)
    }

    override fun <T : Any> getNamed(clazz: Class<T>, qualifier: String): T {
        val mainScope = scope ?: throw RuntimeException("Koin Not Initialized")
        val kotlinClazz = clazz.kotlin
        return mainScope.get(kotlinClazz, StringQualifier(qualifier), null)
    }
}
