package chuyong.springspigot.jpa.type

import com.github.f4b6a3.ulid.Ulid
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class UlidType : UserType<Ulid> {
    override fun equals(x: Ulid?, y: Ulid?): Boolean {
        return x == y
    }

    override fun hashCode(x: Ulid?): Int {
        return x.hashCode()
    }

    override fun getSqlType(): Int {
        return java.sql.Types.VARCHAR
    }

    override fun returnedClass(): Class<Ulid> {
        return Ulid::class.java
    }

    override fun nullSafeGet(
        rs: ResultSet?,
        position: Int,
        session: SharedSessionContractImplementor?,
        owner: Any?,
    ): Ulid? {
        return rs?.getString(position)?.let(Ulid::from)
    }

    override fun isMutable(): Boolean = false

    override fun assemble(cached: Serializable?, owner: Any?): Ulid? {
        return cached as Ulid?
    }

    override fun disassemble(value: Ulid?): Serializable? {
        return value as Serializable?
    }

    override fun deepCopy(value: Ulid?): Ulid? {
        return value
    }

    override fun nullSafeSet(
        st: PreparedStatement?,
        value: Ulid?,
        index: Int,
        session: SharedSessionContractImplementor?,
    ) {
        if(value == null) {
            st?.setNull(index, Types.CHAR)
        }else {
            st?.setString(index, value.toString())
        }
    }

}
