@file:Suppress("UNUSED")
package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.mutableJsonObjectOf
import jp.nephy.penicillin.models.UserStreamUserEvent
import java.util.*

class CustomUserEventBuilder(type: UserEventType): JsonBuilder<UserStreamUserEvent> {
    override val json = mutableJsonObjectOf(
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
