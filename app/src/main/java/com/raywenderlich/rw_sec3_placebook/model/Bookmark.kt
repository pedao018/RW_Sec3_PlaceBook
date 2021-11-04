package com.raywenderlich.rw_sec3_placebook.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Bookmark(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var phone: String = ""
)