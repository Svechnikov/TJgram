package io.svechnikov.tjgram.base.network

import io.svechnikov.tjgram.base.db.BaseDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceToken @Inject constructor(private val db: BaseDatabase) {
    var value: String? = null
        get() {
            if (field == null) {
                field = db.user().user()?.deviceToken
            }
            return field
        }
}