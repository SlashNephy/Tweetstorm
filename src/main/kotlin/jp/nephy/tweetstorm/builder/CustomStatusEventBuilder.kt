@file:Suppress("UNUSED")
package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.mutableJsonObjectOf
import jp.nephy.jsonkt.set
import jp.nephy.penicillin.models.UserStreamStatusEvent
import java.util.*

class CustomStatusEventBuilder(type: StatusEventType): JsonBuilder<UserStreamStatusEvent> {
    override val json = mutableJsonObjectOf(
            "event" to type.key,
            "source" to null,
            "target" to null,
            "target_object" to null,
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

    private var targetObject = CustomStatusBuilder()
    fun targetObject(builder: CustomStatusBuilder.() -> Unit) {
        targetObject.apply(builder)
    }

    private var createdAt: Date? = null
    fun createdAt(date: Date? = null) {
        createdAt = date
    }

    override fun build(): UserStreamStatusEvent {
        json["source"] = source.build()
        json["target"] = target.build()
        json["target_object"] = targetObject.build()
        json["created_at"] = createdAt.toCreatedAt()

        return UserStreamStatusEvent(json)
    }
}
