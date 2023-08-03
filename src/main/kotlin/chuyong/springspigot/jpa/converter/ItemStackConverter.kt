package chuyong.springspigot.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Converter(autoApply = true)
class ItemStackConverter : AttributeConverter<ItemStack?, ByteArray> {
    override fun convertToDatabaseColumn(attribute: ItemStack?): ByteArray {
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

    override fun convertToEntityAttribute(dbData: ByteArray): ItemStack {
        try {
            ByteArrayInputStream(dbData).use { bais -> BukkitObjectInputStream(bais).use { buis -> return buis.readObject() as ItemStack } }
        } catch (ex: Exception) {
            throw RuntimeException()
        }
    }
}
