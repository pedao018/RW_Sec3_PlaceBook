package com.raywenderlich.rw_sec3_placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.raywenderlich.rw_sec3_placebook.model.Bookmark
import com.raywenderlich.rw_sec3_placebook.repository.BookmarkRepo
import com.raywenderlich.rw_sec3_placebook.util.ImageUtils

class MapsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val TAG = "MapsViewModel"

    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarks: LiveData<List<BookmarkView>>? = null

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()
        bookmark.category = getPlaceCategory(place)

        val newId = bookmarkRepo.addBookmark(bookmark)
        image?.let { bookmark.setImage(it, getApplication()) }
        Log.e(TAG, "New bookmark $newId added to the database.")
    }

    fun addBookmark(latLng: LatLng): Long? {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latLng.longitude
        bookmark.latitude = latLng.latitude
        bookmark.category = "Other"
        return bookmarkRepo.addBookmark(bookmark)
    }

    fun getBookmarkMarkerView(): LiveData<List<BookmarkView>>? {
        if (bookmarks == null)
            mapBookmarksToBookmarkView()
        return bookmarks
    }

    private fun mapBookmarksToBookmarkView() {
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks) { repoBookmarks ->
            repoBookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
            }
        }
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark) =
        BookmarkView(
            id = bookmark.id,
            location = LatLng(bookmark.latitude, bookmark.longitude),
            name = bookmark.name,
            phone = bookmark.phone,
            categoryResourceId = bookmarkRepo.getCategoryResourceId(bookmark.category)
        )

    private fun getPlaceCategory(place: Place): String {
        // 1
        var category = "Other"
        val types = place.types

        types?.let { placeTypes ->
            // 2
            if (placeTypes.size > 0) {
                // 3
                val placeType = placeTypes[0]
                category = bookmarkRepo.placeTypeToCategory(placeType)
            }
        }
        // 4
        return category
    }

    data class BookmarkView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0),
        var name: String = "",
        var phone: String = "",
        val categoryResourceId: Int? = null
    ) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
        }
    }
}