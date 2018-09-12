package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.get
import jp.nephy.jsonkt.jsonArray
import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.set
import jp.nephy.penicillin.models.Status
import java.text.SimpleDateFormat
import java.util.*

class CustomStatusBuilder: JsonBuilder<Status> {
    override val json = jsonObject(
            "created_at" to null,
            "id" to null,
            "id_str" to null,
            "text" to "",
            "source" to null,
            "truncated" to false,
            "in_reply_to_status_id" to null,
            "in_reply_to_status_id_str" to null,
            "in_reply_to_user_id" to null,
            "in_reply_to_user_id_str" to null,
            "in_reply_to_screen_name" to null,
            "user" to null,
            "geo" to null,
            "coordinates" to null,
            "place" to null,
            "contributors" to null,
            "is_quote_status" to false,
            "quote_count" to 0,
            "reply_count" to 0,
            "retweet_count" to 0,
            "favorite_count" to 0,
            "entities" to jsonObject(
                    "hashtags" to jsonArray(),
                    "symbols" to jsonArray(),
                    "user_mentions" to jsonArray(),
                    "urls" to jsonArray()
            ),
            "favorited" to false,
            "retweeted" to false,
            "filter_level" to "low",
            "lang" to "ja",
            "timestamp_ms" to null
    )

    fun text(value: String) {
        json["text"] = value
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
        val id = generateId()
        json["id"] = id
        json["id_str"] = id.toString()

        json["source"] = "<a href=\"$sourceUrl\" rel=\"nofollow\">$sourceName</a>"

        json["user"] = user.build().json

        json["created_at"] = createdAt.toCreatedAt()
        json["timestamp_ms"] = (createdAt ?: Date()).time.toString()

        return Status(json)
    }
}

internal fun Date?.toCreatedAt(): String {
    val dateFormatter = SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }
    return dateFormatter.format(this ?: Date())
}

private var id = 100000001L
@Synchronized
internal fun generateId(): Long {
    return id.also {
        id += 2
    }
}
