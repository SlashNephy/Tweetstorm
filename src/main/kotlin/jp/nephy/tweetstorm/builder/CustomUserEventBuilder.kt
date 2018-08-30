package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.set
import jp.nephy.penicillin.models.UserStreamUserEvent
import java.util.*

class CustomUserEventBuilder(type: UserEventType): JsonBuilder<UserStreamUserEvent> {
    companion object {
        fun new(type: UserEventType, builder: CustomUserEventBuilder.() -> Unit): UserStreamUserEvent {
            return CustomUserEventBuilder(type).apply(builder).build()
        }
    }

    override val json = jsonObject(
            "event" to type.key,
            "source" to null,
            "target" to null,
            "created_at" to null
    )

    private var source = CustomUserBuilder()
    fun source(builder: CustomUserBuilder.() -> Unit) {
        source.apply(builder)
    }

    private var target = CustomUserBuilder()
    fun target(builder: CustomUserBuilder.() -> Unit) {
        target.apply(builder)
    }

    private var createdAt: Date? = null
    fun createdAt(date: Date? = null) {
        createdAt = date
    }

    override fun build(): UserStreamUserEvent {
        json["source"] = source.build()
        json["target"] = target.build()
        json["created_at"] = createdAt.toCreatedAt()

        return UserStreamUserEvent(json)
    }
}
