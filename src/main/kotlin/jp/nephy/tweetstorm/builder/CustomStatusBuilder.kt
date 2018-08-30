package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.*
import jp.nephy.penicillin.models.Status
import java.text.SimpleDateFormat
import java.util.*

class CustomStatusBuilder: JsonBuilder<Status> {
    companion object {
        private var id = 1000001L

        @Synchronized
        private fun incrementId(): Long {
            return id.also {
                id += 2
            }
        }

        fun new(builder: CustomStatusBuilder.() -> Unit): Status {
            return CustomStatusBuilder().apply(builder).build()
        }
    }

    override val json = jsonObject(
            "created_at" to null,
            "id" to null,
            "id_str" to null,
            "text" to "",
            "full_text" to "",
            "display_text_range" to jsonArray(),
            "truncated" to false,
            "entities" to jsonObject(
                    "hashtags" to jsonArray(),
                    "symbols" to jsonArray(),
                    "user_mentions" to jsonArray(),
                    "urls" to jsonArray()
            ),
            "source" to null,
            "in_reply_to_status_id" to null,
            "in_reply_to_status_id_str" to null,
            "in_reply_to_user_id" to null,
            "in_reply_to_user_id_str" to null,
            "in_reply_to_screen_name" to null,
            "user" to null,
            "is_quote_status" to false,
            "retweet_count" to 0,
            "favorite_count" to 0,
            "favorited" to false,
            "retweeted" to false,
            "possibly_sensitive" to false,
            "possibly_sensitive_appealable" to false,
            "lang" to "ja",
            "geo" to null,
            "coordinates" to null,
            "place" to null,
            "contributors" to null
    )

    fun text(value: String) {
        json["text"] = value
        json["full_text"] = value
    }
    fun text(operation: () -> Any?) {
        text(operation.invoke().toString())
    }
    fun textBuilder(builder: StringBuilder.() -> Unit) {
        text(buildString(builder))
    }

    private var sourceName = "Tweetstorm"
    private var sourceUrl = "https://github.com/SlashNephy/Tweetstorm"
    fun source(name: String, url: String) {
        sourceName = name
        sourceUrl = url
    }

    private var createdAt: Date? = null
    fun createdAt(date: Date? = null) {
        createdAt = date
    }

    private var user = CustomUserBuilder()
    fun user(builder: CustomUserBuilder.() -> Unit) {
        user.apply(builder)
    }

    fun inReplyTo(statusId: Long, userId: Long, screenName: String) {
        json["in_reply_to_status_id"] = statusId
        json["in_reply_to_status_id_str"] = statusId.toString()
        json["in_reply_to_user_id"] = userId
        json["in_reply_to_user_id_str"] = userId.toString()
        json["in_reply_to_screen_name"] = screenName
    }

    fun alreadyRetweeted() {
        json["retweeted"] = true
    }

    fun alreadyFavorited() {
        json["favorited"] = true
    }

    fun count(retweet: Int = 0, favorite: Int = 0) {
        json["retweet_count"] = retweet
        json["favorite_count"] = favorite
    }

    fun url(url: String, start: Int, end: Int) {
        json["entities"]["urls"].jsonArray.add(jsonObject(
                "display_url" to url.removePrefix("https://").removePrefix("http://"),
                "url" to url,
                "indices" to jsonArray(start, end),
                "expanded_url" to url
        ))
    }

    override fun build(): Status {
        val id = incrementId()
        json["id"] = id
        json["id_str"] = id.toString()

        json["display_text_range"] = jsonArray(0, json["text"].string.length)

        json["source"] = "<a href=\"$sourceUrl\" rel=\"nofollow\">$sourceName</a>"

        json["user"] = user.build().json

        json["created_at"] = createdAt.toCreatedAt()

        return Status(json)
    }
}

internal fun Date?.toCreatedAt(): String {
    val dateFormatter = SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }
    return dateFormatter.format(this ?: Date())
}
