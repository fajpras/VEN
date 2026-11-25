package com.ven.app.ui.screens.home

import androidx.compose.runtime.Immutable
import com.ven.app.R
import com.ven.app.core.localization.AppStrings
import com.ven.app.data.api.models.KaryaItemModel
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.ui.screens.explore.PostsFeedItem
import com.ven.app.util.DateFormatter

@Immutable
data class ComingSoonEvent(
    val id: String,
    val title: String,
    val dateText: String,
    val imageUrl: String? = null,
)

@Immutable
data class PostItem(
    val id: String,
    val username: String,
    val userId: String? = null,
    val userAvatarUrl: String? = null,
    val title: String,
    val category: String? = null,
    val timestampText: String? = null,
    val imageUrl: String? = null,
    val isVideo: Boolean = false,
    val pameranId: String? = null,
    val aspectRatio: Float = 4f / 5f,
    val description: String = "",
)

@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val currentPage: Int = 1,
    val comingSoonEvents: List<ComingSoonEvent> = emptyList(),
    val posts: List<PostItem> = emptyList(),
    val seenPostIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

fun PameranItemModel.toComingSoonEvent(): ComingSoonEvent {
    val dateDisplay = createdAt?.take(10) ?: AppStrings.get(R.string.home_coming_soon)
    return ComingSoonEvent(
        id = idPameran.toString(),
        title = judul,
        dateText = dateDisplay,
        imageUrl = banner,
    )
}

fun KaryaItemModel.toPostItem(): PostItem {
    val author = pengguna?.namaPanggilan ?: pengguna?.nama ?: AppStrings.get(R.string.common_anonymous)
    val relativeTime = DateFormatter.formatRelativeTime(createdAt)
    val videoPost = jenis.equals("video", ignoreCase = true)
    return PostItem(
        id = idKarya,
        username = "@$author",
        userId = pengguna?.idPengguna,
        userAvatarUrl = pengguna?.fotoProfil,
        title = judul,
        category = kategori?.nama,
        timestampText = relativeTime,
        imageUrl = fileUrl ?: file,
        isVideo = videoPost,
        pameranId = idPameran?.toString(),
        aspectRatio = if (videoPost) 16f / 9f else 4f / 5f,
        description = deskripsi ?: "",
    )
}

fun PostItem.toFeedItem(): PostsFeedItem {
    return PostsFeedItem(
        id = id,
        username = username,
        userId = userId,
        userAvatarUrl = userAvatarUrl,
        title = title,
        category = category,
        timestampText = timestampText,
        description = description,
        imageUrl = imageUrl,
        isVideo = isVideo,
        pameranId = pameranId,
        aspectRatio = aspectRatio,
    )
}
