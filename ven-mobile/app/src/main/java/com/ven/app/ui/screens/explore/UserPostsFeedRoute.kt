package com.ven.app.ui.screens.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ven.app.R
import com.ven.app.core.localization.AppStrings
import com.ven.app.data.api.ApiClient
import com.ven.app.data.api.ApiKarya
import com.ven.app.data.api.ApiPencarian
import com.ven.app.ui.screens.profile.ProfileViewModel
import com.ven.app.util.DateFormatter
import kotlinx.coroutines.launch

@Composable
fun UserPostsFeedRoute(
    userId: String,
    initialPostId: String?,
    onBackClick: () -> Unit,
    onRequestMeetingClick: (String) -> Unit,
    onView3dClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onEditPostClick: ((String) -> Unit)? = null,
) {
    val profileViewModel: ProfileViewModel = viewModel()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val apiPencarian = remember { ApiClient.createService(ApiPencarian::class.java) }
    var userPosts by remember(userId) { mutableStateOf<List<PostsFeedItem>>(emptyList()) }
    var isUserLoading by remember(userId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val loadUserPosts: () -> Unit = {
        if (userId.isNotBlank() && userId != "me") {
            scope.launch {
                isUserLoading = true
                try {
                    val response = apiPencarian.getDetailPengguna(userId)
                    if (response.isSuccessful) {
                        val detail = response.body()?.data
                        userPosts = detail?.karya?.map { artwork ->
                            val author = detail.pengguna?.namaPanggilan
                                ?: detail.pengguna?.nama
                                ?: AppStrings.get(R.string.common_creator)
                            val videoPost = artwork.jenis.equals("video", ignoreCase = true)
                            PostsFeedItem(
                                id = artwork.idKarya,
                                username = "@$author",
                                title = artwork.judul,
                                timestampText = DateFormatter.formatRelativeTime(artwork.createdAt),
                                description = artwork.deskripsi.orEmpty(),
                                imageUrl = artwork.fileUrl ?: artwork.file,
                                isVideo = videoPost,
                                userId = detail.pengguna?.idPengguna,
                                userAvatarUrl = detail.pengguna?.fotoProfil,
                                pameranId = artwork.idPameran?.toString(),
                                aspectRatio = if (videoPost) 16f / 9f else 4f / 5f,
                            )
                        }.orEmpty()
                    }
                } catch (_: Exception) {
                    userPosts = emptyList()
                } finally {
                    isUserLoading = false
                }
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId == "me") profileViewModel.muat() else loadUserPosts()
    }

    val isOwnFeed = userId == "me"

    PostsFeedScreen(
        posts = if (isOwnFeed) profileState.feedPosts else userPosts,
        isRefreshing = if (isOwnFeed) profileState.isLoading else isUserLoading,
        onRefresh = {
            if (isOwnFeed) profileViewModel.muat() else loadUserPosts()
        },
        onBackClick = onBackClick,
        onRequestMeetingClick = onRequestMeetingClick,
        onView3dClick = onView3dClick,
        initialPostId = initialPostId,
        onUserClick = onUserClick,
        onEditPostClick = if (isOwnFeed) onEditPostClick else null,
        onDeletePostClick = if (isOwnFeed) {
            { postId ->
                scope.launch {
                    ApiKarya.hapus(postId).onSuccess {
                        profileViewModel.muat()
                    }
                }
            }
        } else {
            null
        },
    )
}
