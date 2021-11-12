package com.raywenderlich.rw_sec3_placebook.model

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raywenderlich.rw_sec3_placebook.util.ImageUtils

@Entity
data class Bookmark(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var phone: String = "",
    var notes:  String = ""
) {
    fun setImage(image: Bitmap, context: Context) {
        id?.let { id ->
            ImageUtils.saveBitmapToFile(
                context, image, generateImageFilename(id)
            )
        }
    }

    companion object {
        fun generateImageFilename(id: Long): String {
            return "bookmark$id.png"
        }
    }
}