package jp.nephy.tweetstorm.builder

enum class StatusEventType(val key: String) {
    Favorite("favorite"), Unfavorite("unfavorite"),
    FavoritedRetweet("favorited_retweet"), RetweetedRetweet("retweeted_retweet"),
    QuotedTweet("quoted_tweet")
}

enum class ListEventType(val key: String) {
    ListCreated("list_created"), ListDestroyed("list_destroyed"), ListUpdated("list_updated"),
    ListMemberAdded("list_member_added"), ListMemberRemoved("list_member_removed"),
    ListUserSubscribed("list_user_subscribed"), ListUserUnsubscribed("list_user_unsubscribed")
}

enum class UserEventType(val key: String) {
    Follow("follow"), Unfollow("unfollow"),
    Block("block"), Unblock("unblock"),
    Mute("mute"), Unmute("unmute"),
    UserUpdate("user_update")
}
