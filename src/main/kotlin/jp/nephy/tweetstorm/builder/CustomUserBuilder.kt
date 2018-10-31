@file:Suppress("UNUSED")
package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.mutableJsonArrayOf
import jp.nephy.jsonkt.mutableJsonObjectOf
import jp.nephy.penicillin.models.User
import java.util.*

class CustomUserBuilder: JsonBuilder<User> {
    companion object {
        private const val userId = 1L
    }

    override val json = mutableJsonObjectOf(
            "id" to userId,
            "id_str" to userId.toString(),
            "name" to "Tweetstorm",
            "screen_name" to "Tweetstorm",
            "location" to null,
            "description" to "This account is dummy and is used to deliver internal messages.",
            "url" to "https://github.com/SlashNephy/Tweetstorm",
            "entities" to mutableJsonObjectOf(
                    "url" to mutableJsonObjectOf(
                            "urls" to mutableJsonArrayOf(
                                    mutableJsonObjectOf(
                                        "display_url" to "github.com/SlashNephy/Tweetstorm",
                                        "url" to "https://t.co/Cn0EQY6Yzd",
                                        "indices" to mutableJsonArrayOf(0, 23),
                                        "expanded_url" to "https://github.com/SlashNephy/Tweetstorm"
                                    )
                            )
                    ),
                    "description" to mutableJsonObjectOf(
                            "urls" to mutableJsonArrayOf()
                    )
            ),
            "protected" to false,
            "followers_count" to 0,
            "friends_count" to 0,
            "listed_count" to 0,
            "created_at" to null,
            "favourites_count" to 0,
            "utc_offset" to null,
            "time_zone" to null,
            "geo_enabled" to false,
            "verified" to false,
            "statuses_count" to 0,
            "lang" to "ja",
            "is_translator" to false,
            "is_translation_enabled" to false,
            "profile_background_color" to "000000",
            "profile_background_image_url" to "http://abs.twimg.com/images/themes/theme1/bg.png",
            "profile_background_image_url_https" to "https://abs.twimg.com/images/themes/theme1/bg.png",
            "profile_background_tile" to false,
            "profile_image_url" to "http://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png",
            "profile_image_url_https" to "https://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png",
            "profile_banner_url" to null,
            "profile_link_color" to "FFFFFF",
            "profile_sidebar_border_color" to "000000",
            "profile_sidebar_fill_color" to "000000",
            "profile_text_color" to "000000",
            "profile_use_background_image" to false,
            "has_extended_profile" to false,
            "default_profile" to false,
            "default_profile_image" to false,
            "following" to false,
            "follow_request_sent" to false,
            "notifications" to false,
            "contributors_enabled" to false
    )

    fun name(value: String) {
        json["name"] = value
    }
    fun screenName(value: String) {
        json["screen_name"] = value
    }

    fun location(value: String) {
        json["location"] = value
    }

    fun isProtected() {
        json["protected"] = true
    }
    fun isVerified() {
        json["verified"] = true
    }

    fun count(friends: Int = 0, followers: Int = 0, statuses: Int = 0, favorites: Int = 0, listed: Int = 0) {
        json["friends_count"] = friends
        json["followers_count"] = followers
        json["statuses_count"] = statuses
        json["favourites_count"] = favorites
        json["listed_count"] = listed
    }

    fun icon(url: String) {
        json["profile_image_url"] = url.replace("https://", "http://")
        json["profile_image_url_https"] = url.replace("http://", "https://")
    }

    private var createdAt: Date? = null
    fun createdAt(date: Date? = null) {
        createdAt = date
    }

    override fun build(): User {
        json["created_at"] = createdAt.toCreatedAt()

        return User(json)
    }
}
