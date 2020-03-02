package kotlinx.io.json.data

import com.fasterxml.jackson.annotation.*
import kotlinx.serialization.*

@Serializable
data class Twitter(
    val statuses: List<TwitterStatus>,
    val search_metadata: TwitterSearchMetaData
)

@Serializable
data class TwitterStatus(
    val metadata: TwitterStatusMetadata,
    val created_at: String,
    val id: Long,
    val id_str: String,
    val text: String,
    val source: String,
    val truncated: Boolean,
    val in_reply_to_status_id: Long?,
    val in_reply_to_status_id_str: Long?,
    val in_reply_to_user_id: Long?,
    val in_reply_to_user_id_str: Long?,
    val in_reply_to_screen_name: String?,
    val user: TwitterUser,
    val geo: String?,
    val coordinates: String?,
    val place: String?,
    val contributors: List<String>?,
    val retweeted_status: TwitterStatus? = null,
    val retweet_count: Int,
    val favorite_count: Int,
    val entities: TwitterStatusEntities,
    val favorited: Boolean,
    val retweeted: Boolean,
    val lang: String,
    val possibly_sensitive: Boolean? = null
)

@Serializable
data class TwitterRetweetedStatus(
    val metadata: TwitterStatusMetadata
)

@Serializable
data class TwitterStatusEntities(
    val hashtags: List<TwitterHashtag>,
    val symbols: List<String>,
    val urls: List<TwitterUrl>,
    val user_mentions: List<TwitterUserMention>,
    val media: List<TwitterMedia>? = null
)

@Serializable
data class TwitterMedia(
    val id: Long,
    val id_str: String,
    val url: String,
    val media_url: String,
    val media_url_https: String,
    val expanded_url: String,
    val display_url: String,
    val indices: List<Int>,
    val type: String,
    val sizes: TwitterSizeType,
    val source_status_id: Long? = null,
    val source_status_id_str: String? = null
)

@Serializable
data class TwitterSizeType(
    val large: TwitterSize,
    val medium: TwitterSize,
    val thumb: TwitterSize,
    val small: TwitterSize
)

@Serializable
data class TwitterSize(val w: Int, val h: Int, val resize: String)

@Serializable
data class TwitterHashtag(
    val text: String,
    val indices: List<Int>
)

@Serializable
data class TwitterUserMention(
    val screen_name: String,
    val name: String,
    val id: Long,
    val id_str: String,
    val indices: List<Int>
)

@Serializable
data class TwitterUrls(val urls: List<TwitterUrl>)

@Serializable
data class TwitterUrl(
    val url: String,
    val expanded_url: String,
    val display_url: String,
    val indices: List<Int>
)

@Serializable
data class TwitterStatusMetadata(
    val result_type: String,
    val iso_language_code: String
)

@Serializable
data class TwitterUser(
    val id: Long,
    val id_str: String,
    val name: String,
    val screen_name: String,
    val location: String,
    val description: String,
    val url: String?,
    val entities: TwitterUserEntities,
    val protected: Boolean,
    val followers_count: Int,
    val friends_count: Int,
    val listed_count: Int,
    val created_at: String,
    val favourites_count: Int,
    val utc_offset: String?,
    val time_zone: String?,
    val geo_enabled: Boolean,
    val verified: Boolean,
    val statuses_count: Int,
    val lang: String,
    val contributors_enabled: Boolean,
    val is_translator: Boolean,
    val is_translation_enabled: Boolean,
    val profile_background_color: String,
    val profile_background_image_url: String,
    val profile_background_image_url_https: String,
    val profile_background_tile: Boolean,
    val profile_image_url: String,
    val profile_image_url_https: String,
    val profile_banner_url: String? = null,
    val profile_link_color: String,
    val profile_sidebar_border_color: String,
    val profile_sidebar_fill_color: String,
    val profile_text_color: String,
    val profile_use_background_image: Boolean,
    val default_profile: Boolean,
    val default_profile_image: Boolean,
    val following: Boolean,
    val follow_request_sent: Boolean,
    val notifications: Boolean
)

@Serializable
data class TwitterUserEntities(
    val url: TwitterUrls? = null,
    val description: TwitterUrls
)

@Serializable
data class TwitterSearchMetaData(
    val completed_in: Float,
    val max_id: Long,
    val max_id_str: String,
    val next_results: String,
    val query: String,
    val refresh_url: String,
    val count: Int,
    val since_id: Long,
    val since_id_str: String
)
