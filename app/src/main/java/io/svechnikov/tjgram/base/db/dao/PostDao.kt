package io.svechnikov.tjgram.base.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.svechnikov.tjgram.base.db.entities.Post

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(posts : List<Post>)

    @Query("DELETE FROM posts")
    fun clearAll()

    @Query("DELETE FROM posts WHERE sorting = :sorting")
    fun clear(sorting: Post.Sorting)

    @Query("SELECT * FROM posts WHERE thumbnailUrl is NOT NULL AND sorting = :sorting ORDER BY id ASC")
    fun posts(sorting: Post.Sorting): DataSource.Factory<Int, Post>

    @Query("SELECT 1 FROM posts WHERE isFinal = 1 AND sorting = :sorting")
    fun hasFinalPost(sorting: Post.Sorting): Boolean

    @Query("SELECT COUNT(*) FROM posts WHERE sorting = :sorting")
    fun count(sorting: Post.Sorting): Int

    @Query("UPDATE posts SET isFinal = 1 WHERE id = :id")
    fun setFinal(id: Int)

    @Query("SELECT id FROM posts ORDER BY id DESC LIMIT 1")
    fun getLastId(): Int

    @Query("UPDATE posts SET likes = :likes, isLiked = :isLiked WHERE postId = :postId")
    fun updateLikes(postId: Long, likes: Int, isLiked: Int)

    @Query("UPDATE posts SET isLiked = :isLiked WHERE postId = :postId")
    fun updateIsLiked(postId: Long, isLiked: Int)

    @Query("SELECT postId FROM posts WHERE id = :id")
    fun getPostIdById(id: Int): Long
}