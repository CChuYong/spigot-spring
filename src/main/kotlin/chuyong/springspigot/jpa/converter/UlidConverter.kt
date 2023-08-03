package chuyong.springspigot.jpa.converter

import com.github.f4b6a3.ulid.Ulid
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class UlidConverter : AttributeConverter<Ulid, String> {
    override fun convertToDatabaseColumn(attribute: Ulid?): String? {
        return attribute?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): Ulid? {
        return dbData?.let(Ulid::from)
    }
}
