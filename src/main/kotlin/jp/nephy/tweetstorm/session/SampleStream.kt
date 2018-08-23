package jp.nephy.tweetstorm.session

import io.ktor.http.Parameters
import jp.nephy.jsonkt.*
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private val sampleStatus = jsonObject(
        "created_at" to "",
        "id" to 1030122500561166336,
        "id_str" to "1030122500561166336",
        "text" to "",
        "truncated" to false,
        "entities" to jsonObject(
                "hashtags" to jsonArray(),
                "symbols" to jsonArray(),
                "user_mentions" to jsonArray(),
                "urls" to jsonArray()
        ),
        "source" to "<a href=\"https://about.twitter.com/products/tweetdeck\" rel=\"nofollow\">Tweetdeck</a>",
        "in_reply_to_status_id" to null,
        "in_reply_to_status_id_str" to null,
        "in_reply_to_user_id" to null,
        "in_reply_to_user_id_str" to null,
        "in_reply_to_screen_name" to null,
        "user" to jsonObject(
                "id" to 1,
                "id_str" to "1",
                "name" to "Twitter API",
                "screen_name" to "TwitterAPI",
                "location" to "...",
                "description" to "...",
                "url" to "https://developer.twitter.com",
                "entities" to jsonObject(
                        "url" to jsonObject(
                                "urls" to jsonArray()
                        ),
                        "description" to jsonObject(
                                "urls" to jsonArray()
                        )
                ),
                "protected" to true,
                "followers_count" to 0,
                "fast_followers_count" to 0,
                "normal_followers_count" to 0,
                "friends_count" to 99999999,
                "listed_count" to 0,
                "created_at" to "Sun Sep 24 04:53:44 +0000 2017",
                "favourites_count" to 114514,
                "utc_offset" to null,
                "time_zone" to null,
                "geo_enabled" to false,
                "verified" to false,
                "statuses_count" to 999999999,
                "media_count" to 19191919,
                "lang" to "ja",
                "contributors_enabled" to false,
                "is_translator" to false,
                "is_translation_enabled" to false,
                "profile_background_color" to "000000",
                "profile_background_image_url" to "http://abs.twimg.com/images/themes/theme1/bg.png",
                "profile_background_image_url_https" to "https://abs.twimg.com/images/themes/theme1/bg.png",
                "profile_background_tile" to false,
                "profile_image_url" to "http://pbs.twimg.com/profile_images/942858479592554497/BbazLO9L_normal.jpg",
                "profile_image_url_https" to "https://pbs.twimg.com/profile_images/942858479592554497/BbazLO9L_normal.jpg",
                "profile_banner_url" to "https://pbs.twimg.com/profile_banners/6253282/1497491515/1500x500",
                "profile_link_color" to "FFFFFF",
                "profile_sidebar_border_color" to "000000",
                "profile_sidebar_fill_color" to "000000",
                "profile_text_color" to "000000",
                "profile_use_background_image" to false,
                "has_extended_profile" to true,
                "default_profile" to false,
                "default_profile_image" to false,
                "pinned_tweet_ids" to jsonArray(),
                "pinned_tweet_ids_str" to jsonArray(),
                "has_custom_timelines" to true,
                "can_media_tag" to true,
                "followed_by" to true,
                "following" to true,
                "follow_request_sent" to false,
                "notifications" to false,
                "business_profile_state" to "none",
                "translator_type" to "none",
                "require_some_consent" to false
        ),
        "geo" to null,
        "coordinates" to null,
        "place" to null,
        "contributors" to null,
        "is_quote_status" to false,
        "retweet_count" to 334334334,
        "favorite_count" to 1145141919,
        "favorited" to true,
        "retweeted" to true,
        "lang" to "ja",
        "supplemental_language" to null
)

class SampleStream(writer: Writer, query: Parameters): Stream(writer, query) {
    override fun handle() {
        val formatter = SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        while (isAlive) {
            sampleStatus["created_at"] = formatter.format(Date())
            sampleStatus["text"] = UUID.randomUUID().toString()
            sampleStatus["id"] = sampleStatus["id"].long + 1
            sampleStatus["id_str"] = sampleStatus["id"].long.toString()

            send(JsonKt.toJsonString(sampleStatus))
            TimeUnit.SECONDS.sleep(3)
        }
    }
}
