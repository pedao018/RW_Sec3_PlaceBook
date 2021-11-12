package com.raywenderlich.rw_sec3_placebook.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.raywenderlich.rw_sec3_placebook.R
import com.raywenderlich.rw_sec3_placebook.databinding.ActivityBookmarkDetailsBinding
import com.raywenderlich.rw_sec3_placebook.viewmodel.BookmarkDetailsViewModel

class BookmarkDetailsActivity : AppCompatActivity() {
    private lateinit var databinding: ActivityBookmarkDetailsBinding
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_details)
        setupToolbar()
        getIntentData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_save -> {
            saveChanges()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }

    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(
            MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0
        )
        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this, {
            bookmarkDetailsView = it
            databinding.bookmarkDetailsView = it
            populateImageView()
        })
    }

    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let {
                databinding.imageViewPlace.setImageBitmap(placeImage)
            }
        }
    }

    private fun saveChanges() {
        val name = databinding.editTextName.text.toString()
        if (name.isEmpty())
            return
        bookmarkDetailsView?.let { bookmarkDetailsView ->
            bookmarkDetailsView.name = name
            bookmarkDetailsView.notes = databinding.editTextNotes.text.toString()
            bookmarkDetailsView.address = databinding.editTextAddress.text.toString()
            bookmarkDetailsView.phone = databinding.editTextPhone.text.toString()
            bookmarkDetailsViewModel.updateBookmark(bookmarkDetailsView)
        }
        finish()
    }

}