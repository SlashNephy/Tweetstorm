package jp.nephy.tweetstorm.builder

import jp.nephy.penicillin.models.*

fun newStatus(builder: CustomStatusBuilder.() -> Unit): Status {
    return CustomStatusBuilder().apply(builder).build()
}

fun newStatusEvent(type: StatusEventType, builder: CustomStatusEventBuilder.() -> Unit): UserStreamStatusEvent {
    return CustomStatusEventBuilder(type).apply(builder).build()
}

fun newList(builder: CustomListBuilder.() -> Unit): TwitterList {
    return CustomListBuilder().apply(builder).build()
}

fun newListEvent(type: ListEventType, builder: CustomListEventBuilder.() -> Unit): UserStreamListEvent {
    return CustomListEventBuilder(type).apply(builder).build()
}

fun newUser(builder: CustomUserBuilder.() -> Unit): User {
    return CustomUserBuilder().apply(builder).build()
}

fun newUserEvent(type: UserEventType, builder: CustomUserEventBuilder.() -> Unit): UserStreamUserEvent {
    return CustomUserEventBuilder(type).apply(builder).build()
}

fun newDirectMessage(builder: CustomDirectMessageBuilder.() -> Unit): DirectMessage {
    return CustomDirectMessageBuilder().apply(builder).build()
}
