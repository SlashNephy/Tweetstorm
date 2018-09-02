package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.set
import jp.nephy.penicillin.models.DirectMessage
import java.util.*

class CustomDirectMessageBuilder: JsonBuilder<DirectMessage> {
    override val json = jsonObject(
            "created_at" to null,
            "entities" to jsonObject(),
            "id" to null,
            "id_str" to null,
            "read" to false,
            "recipient" to null,
            "recipient_id" to null,
            "recipient_id_str" to null,
            "sender" to null,
            "sender_id" to null,
            "sender_id_str" to null,
            "sender_screen_name" to null,
            "text" to null
    )

    private var createdAt: Date? = null
    fun createdAt(date: Date? = null) {
        createdAt = date
    }

    fun read() {
        json["read"] = true
    }

    private val recipientBuilder = CustomUserBuilder()
    fun recipient(builder: CustomUserBuilder.() -> Unit) {
        recipientBuilder.apply(builder)
    }

    private val senderBuilder = CustomUserBuilder()
    fun sender(builder: CustomUserBuilder.() -> Unit) {
        senderBuilder.apply(builder)
    }

    fun text(text: () -> Any?) {
        json["text"] = text()?.toString().orEmpty()
    }

    override fun build(): DirectMessage {
        json["created_at"] = createdAt.toCreatedAt()

        val id = generateId()
        json["id"] = id
        json["id_str"] = id.toString()

        val recipient = recipientBuilder.build()
        json["recipient"] = recipient.json
        json["recipient_id"] = recipient.id
        json["recipient_id_str"] = recipient.idStr

        val sender = senderBuilder.build()
        json["sender"] = sender.json
        json["sender_id"] = sender.id
        json["sender_id_str"] = sender.idStr
        json["sender_screen_name"] = sender.screenName

        return DirectMessage(json)
    }
}
