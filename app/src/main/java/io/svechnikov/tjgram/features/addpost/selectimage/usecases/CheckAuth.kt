package io.svechnikov.tjgram.features.addpost.selectimage.usecases

import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.db.BaseDatabase
import kotlinx.coroutines.delay
import javax.inject.Inject

class CheckAuth @Inject constructor(
    private val db: BaseDatabase): UseCase<Unit, Boolean>() {

    override suspend fun run(params: Unit): Boolean {
        return db.user().user() != null
    }
}