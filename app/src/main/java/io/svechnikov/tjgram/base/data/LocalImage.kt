package io.svechnikov.tjgram.base.data

data class LocalImage(val id: Int,
                      val path: String,
                      val width: Int,
                      val height: Int,
                      val thumbPath: String?,
                      val size: Int)