package jp.nephy.tweetstorm.builder

internal interface EventType {
    val key: String
}

enum class StatusEventType(override val key: String): EventType {
    Favorite("favorite"), Unfavorite("unfavorite"),
    FavoritedRetweet("favorited_retweet"), RetweetedRetweet("retweeted_retweet"),
    QuotedTweet("quoted_tweet")
}

enum class ListEventType(override val key: String): EventType {
    ListCreated("list_created"), ListDestroyed("list_destroyed"), ListUpdated("list_updated"),
    ListMemberAdded("list_member_added"), ListMemberRemoved("list_member_removed"),
    ListUserSubscribed("list_user_subscribed"), ListUserUnsubscribed("list_user_unsubscribed")
}

enum class UserEventType(override val key: String): EventType {
    Follow("follow"), Unfollow("unfollow"),
    Block("block"), Unblock("unblock"),
    Mute("mute"), Unmute("unmute"),
    UserUpdate("user_update")
}

internal fun String.toEventType(): EventType? {
    val statusEvent = StatusEventType.values().find { it.key == this }
    if (statusEvent != null) {
        return statusEvent
    }
    val listEvent = ListEventType.values().find { it.key == this }
    if (listEvent != null) {
        return listEvent
    }
    return UserEventType.values().find { it.key == this }
}
