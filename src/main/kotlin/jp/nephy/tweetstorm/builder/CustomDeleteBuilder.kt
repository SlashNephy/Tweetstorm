package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.get
import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.set
import jp.nephy.penicillin.models.StreamDelete
import java.util.*
import kotlin.properties.Delegates

class CustomDeleteBuilder: JsonBuilder<StreamDelete> {
    override val json = jsonObject(
            "delete" to jsonObject(
                    "status" to jsonObject(
                            "id" to null,
                            "id_str" to null,
                            "user_id" to null,
                            "user_id_str" to null
                    ),
                    "timestamp_ms" to null
            )
    )

    private var statusId by Delegates.notNull<Long>()
    fun status(id: Long) {
        statusId = id
    }

    private var userId by Delegates.notNull<Long>()
    fun author(id: Long) {
        userId = id
    }

    private var createdAt: Date? = null
    fun timestamp(date: Date? = null) {
        createdAt = date
    }

    override fun build(): StreamDelete {
        json["delete"]["status"]["id"] = statusId
        json["delete"]["status"]["id_str"] = statusId.toString()
        json["delete"]["status"]["user_id"] = userId
        json["delete"]["status"]["user_id_str"] = userId.toString()
        json["delete"]["timestamp_ms"] = (createdAt ?: Date()).time.toString()

        return StreamDelete(json)
    }
}