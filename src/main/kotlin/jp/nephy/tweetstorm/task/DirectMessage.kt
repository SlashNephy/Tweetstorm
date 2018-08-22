package jp.nephy.tweetstorm.task

import jp.nephy.penicillin.model.DirectMessage
import jp.nephy.tweetstorm.TaskManager

class DirectMessage(override val manager: TaskManager): FetchTask<DirectMessage>() {
    override fun provide(data: DirectMessage) {
        manager.emit(data)
    }

    override fun fetch() {
        // TODO
    }
}
