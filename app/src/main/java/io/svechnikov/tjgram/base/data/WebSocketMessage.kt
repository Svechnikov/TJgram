package io.svechnikov.tjgram.base.data

data class WebSocketMessage(val type: String,
                            val contentId: Long,
                            val count: Int,
                            val id: Long,
                            val state: Int,
                            val userHash: String)