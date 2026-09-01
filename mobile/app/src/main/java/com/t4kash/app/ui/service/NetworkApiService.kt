package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.CreateNetworkCommentRequest
import com.t4kash.app.ui.model.CreateNetworkPostRequest
import com.t4kash.app.ui.model.NetworkCommentDto
import com.t4kash.app.ui.model.NetworkPostDto
import com.t4kash.app.ui.model.NetworkReactionRequest
import com.t4kash.app.ui.model.UpdateNetworkCommentRequest
import com.t4kash.app.ui.model.UpdateNetworkPostRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NetworkApiService {
    @GET("network/feed")
    suspend fun getFeed(
        @Query("alcance") scope: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): List<NetworkPostDto>

    @GET("network/saved")
    suspend fun getSavedPosts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): List<NetworkPostDto>

    @POST("network/posts")
    suspend fun createPost(
        @Body request: CreateNetworkPostRequest
    ): NetworkPostDto

    @PUT("network/posts/{postId}")
    suspend fun updatePost(
        @Path("postId") postId: Int,
        @Body request: UpdateNetworkPostRequest
    ): NetworkPostDto

    @DELETE("network/posts/{postId}")
    suspend fun deletePost(
        @Path("postId") postId: Int
    ): NetworkPostDto

    @PUT("network/posts/{postId}/reaction")
    suspend fun setReaction(
        @Path("postId") postId: Int,
        @Body request: NetworkReactionRequest
    ): NetworkPostDto

    @DELETE("network/posts/{postId}/reaction")
    suspend fun removeReaction(
        @Path("postId") postId: Int
    ): NetworkPostDto

    @PUT("network/posts/{postId}/saved")
    suspend fun savePost(
        @Path("postId") postId: Int
    ): NetworkPostDto

    @DELETE("network/posts/{postId}/saved")
    suspend fun removeSavedPost(
        @Path("postId") postId: Int
    ): NetworkPostDto

    @GET("network/posts/{postId}/comments")
    suspend fun getComments(
        @Path("postId") postId: Int,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): List<NetworkCommentDto>

    @POST("network/posts/{postId}/comments")
    suspend fun createComment(
        @Path("postId") postId: Int,
        @Body request: CreateNetworkCommentRequest
    ): NetworkCommentDto

    @PUT("network/comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: Int,
        @Body request: UpdateNetworkCommentRequest
    ): NetworkCommentDto

    @DELETE("network/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: Int
    ): NetworkCommentDto
}

