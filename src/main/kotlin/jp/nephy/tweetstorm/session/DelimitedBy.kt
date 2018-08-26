package jp.nephy.tweetstorm.session

enum class DelimitedBy {
    Default, Length;

    companion object {
        fun byName(name: String): DelimitedBy {
            return values().find { it.name.equals(name, true) } ?: DelimitedBy.Default
        }
    }
}
