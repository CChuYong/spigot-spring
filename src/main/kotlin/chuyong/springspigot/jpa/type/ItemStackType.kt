package chuyong.springspigot.jpa.type

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class ItemStackType: UserType<ItemStack> {
    override fun equals(x: ItemStack?, y: ItemStack?): Boolean {
        return x == y
    }

    override fun hashCode(x: ItemStack?): Int {
        return x.hashCode()
    }

    override fun getSqlType(): Int {
        return Types.BLOB
    }

    override fun returnedClass(): Class<ItemStack> {
        return ItemStack::class.java
    }

    override fun nullSafeGet(
        rs: ResultSet?,
        position: Int,
        session: SharedSessionContractImplementor?,
        owner: Any?
    ): ItemStack? {
        return rs?.getBytes(position)?.let { convertToEntityAttribute(it) }
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun assemble(cached: Serializable?, owner: Any?): ItemStack? {
        return cached as ItemStack?
    }

    override fun disassemble(value: ItemStack?): Serializable? {
        return value as Serializable?
    }

    override fun deepCopy(value: ItemStack?): ItemStack? {
        return value?.clone()
    }

    override fun nullSafeSet(
        st: PreparedStatement?,
        value: ItemStack?,
        index: Int,
        session: SharedSessionContractImplementor?
    ) {
        if(value != null)
            st?.setBytes(index, convertToDatabaseColumn(value))
        else
            st?.setNull(index, Types.BLOB)
    }

    private fun convertToDatabaseColumn(attribute: ItemStack?): ByteArray {
        try {
            ByteArrayOutputStream().use { baos ->
                BukkitObjectOutputStream(baos).use { buos ->
                    buos.writeObject(attribute)
                    return baos.toByteArray()
                }
            }
        } catch (ex: Exception) {
            throw RuntimeException()
        }
    }

    private fun convertToEntityAttribute(dbData: ByteArray): ItemStack {
        try {
            ByteArrayInputStream(dbData).use { bais -> BukkitObjectInputStream(bais).use { buis -> return buis.readObject() as ItemStack } }
        } catch (ex: Exception) {
            throw RuntimeException()
        }
    }
}
