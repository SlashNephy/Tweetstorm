@file:Suppress("UNUSED")
package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.mutableJsonObjectOf
import jp.nephy.penicillin.models.TwitterList
import java.util.*

class CustomListBuilder: JsonBuilder<TwitterList> {
    override val json = mutableJsonObjectOf(
            "created_at" to null,
            "description" to "Tweetstorm",
            "following" to false,
            "full_name" to "Tweetstorm",
            "id" to null,
            "id_str" to null,
            "member_count" to 0,
            "mode" to "public",
            "name" to "Tweetstorm",
            "slag" to "Tweetstorm",
            "subscriber_count" to 0,
            "uri" to "Tweetstorm/Tweetstorm"
    )

    private var createdAt: Date? = null
    fun createdAt(date: Date? = null) {
        createdAt = date
    }

    fun description(text: () -> Any?) {
        json["description"] = text()?.toString().orEmpty()
    }

    fun following() {
        json["following"] = true
    }

    fun name(shortName: String, fullName: String, slug: String, uri: String) {
        json["name"] = shortName
        json["full_name"] = fullName
        json["slug"] = slug
        json["uri"] = uri
    }

    fun count(member: Int = 0, subscriber: Int = 0) {
        json["member_count"] = member
        json["subscriber_count"] = subscriber
    }

    override fun build(): TwitterList {
        json["created_at"] = createdAt.toCreatedAt()

        val id = generateId()
        json["id"] = id
        json["id_str"] = id.toString()

        return TwitterList(json)
    }
}
