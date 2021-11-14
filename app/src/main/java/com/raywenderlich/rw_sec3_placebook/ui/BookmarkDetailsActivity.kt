package com.raywenderlich.rw_sec3_placebook.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.raywenderlich.rw_sec3_placebook.R
import com.raywenderlich.rw_sec3_placebook.databinding.ActivityBookmarkDetailsBinding
import com.raywenderlich.rw_sec3_placebook.util.ImageUtils
import com.raywenderlich.rw_sec3_placebook.viewmodel.BookmarkDetailsViewModel
import java.io.File
import java.net.URLEncoder

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {
    private lateinit var databinding: ActivityBookmarkDetailsBinding
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null
    private var photoFile: File? = null
    private lateinit var startCaptureForResult: ActivityResultLauncher<Intent>
    private lateinit var startSelectImageForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_details)
        setupToolbar()
        setupFab()
        getIntentData()

        startCaptureForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    //You return early from the method if there is no photoFile defined.
                    val photoFile = photoFile ?: return@registerForActivityResult

                    //Thu hồi lại permission: The permissions you set before are now revoked since they’re no longer needed.
                    val uri = FileProvider.getUriForFile(
                        this,
                        "com.raywenderlich.rw_sec3_placebook.fileprovider",
                        photoFile
                    )
                    revokeUriPermission(
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    //getImageWithPath() is called to get the image from the new photo path, and updateImage() is called to update the bookmark image
                    val image = getImageWithPath(photoFile.absolutePath)
                    val bitmap = ImageUtils.rotateImageIfRequired(this, image, uri)
                    updateImage(bitmap)
                }
            }
        startSelectImageForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data //Intent
                    if (data != null && data.data != null) {
                        val imageUri = data.data as Uri
                        val image = getImageWithAuthority(imageUri)
                        image?.let {
                            val bitmap = ImageUtils.rotateImageIfRequired(this, it, imageUri)
                            updateImage(bitmap)
                        }
                    }
                }
            }
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
        R.id.action_delete -> {
            deleteBookmark()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }

    private fun setupFab() {
        databinding.fab.setOnClickListener { sharePlace() }
    }

    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(
            MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0
        )
        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this, {
            bookmarkDetailsView = it
            databinding.bookmarkDetailsView = it
            populateImageView()
            populateCategoryList()
        })
    }

    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let {
                databinding.imageViewPlace.setImageBitmap(placeImage)
            }
        }
        databinding.imageViewPlace.setOnClickListener { replaceImage() }
    }

    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    //This method uses the new decodeFileSize method to load the downsampled image and return it.
    private fun getImageWithPath(filePath: String) = ImageUtils.decodeFileToSize(
        filePath,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height)
    )

    private fun getImageWithAuthority(uri: Uri) = ImageUtils.decodeUriStreamToSize(
        uri,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height),
        this
    )

    private fun updateImage(image: Bitmap) {
        bookmarkDetailsView?.let {
            databinding.imageViewPlace.setImageBitmap(image)
            it.setImage(this, image)
        }
    }

    private fun deleteBookmark() {
        val bookmarkView = bookmarkDetailsView ?: return
        AlertDialog.Builder(this)
            .setMessage("Delete?")
            .setPositiveButton("Ok") { _, _ ->
                bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .create().show()
    }

    private fun populateCategoryList() {
        val bookmarkView = bookmarkDetailsView ?: return
        val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(bookmarkView.category)
        resourceId?.let { databinding.imageViewCategory.setImageResource(it) }
        val categories = bookmarkDetailsViewModel.getCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        databinding.spinnerCategory.adapter = adapter
        val placeCategory = bookmarkView.category
        databinding.spinnerCategory.setSelection(adapter.getPosition(placeCategory))

        /*
        The need to use spinnerCategory.post is due to an unfortunate side effect in Android where onItemSelected() is always called once with an initial position of 0.
        This causes the spinner to reset back to the first category regardless of the selection you set programmatically.
        Using post causes the code block to be placed on the main thread queue, and the execution of the code inside the braces gets delayed until the next message loop.
        This eliminates the initial call by Android to onItemSelected().
        * */
        databinding.spinnerCategory.post {
            databinding.spinnerCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View,
                        position: Int,
                        id: Long
                    ) {
                        val category = parent.getItemAtPosition(position) as String
                        val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(category)
                        resourceId?.let {
                            databinding.imageViewCategory.setImageResource(it)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        // NOTE: This method is required but not used.
                    }
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
            bookmarkDetailsView.category = databinding.spinnerCategory.selectedItem as String
            bookmarkDetailsViewModel.updateBookmark(bookmarkDetailsView)
        }
        finish()
    }

    override fun onCaptureClick() {
        photoFile = null
        try {
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: java.io.IOException) {
            return
        }

        photoFile?.let { photoFile ->
            //FileProvider.getUriForFile() is called to get a Uri for the temporary photo file.
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.raywenderlich.rw_sec3_placebook.fileprovider",
                photoFile
            )

            //A new Intent is created with the ACTION_IMAGE_CAPTURE action.
            // This Intent is used to display the camera viewfinder and allow the user to snap a new photo.
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            //The photoUri is added as an extra on the Intent, so the Intent knows where to save the full-size image captured by the user.
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            //Temporary write permissions on the photoUri are given to the Intent
            val intentActivities = packageManager.queryIntentActivities(
                captureIntent, PackageManager.MATCH_DEFAULT_ONLY
            )
            intentActivities.map { it.activityInfo.packageName }
                .forEach {
                    grantUriPermission(
                        it, photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            startCaptureForResult.launch(captureIntent)
        }

    }

    override fun onPickClick() {
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startSelectImageForResult.launch(pickIntent)
    }

    /*
    Android allows you to share data with other apps using an Intent with an ACTION_SEND action.
    All you need to do is provide the data.
    Android figures out the apps that support your data type and presents the user with a list of choices.
    * */
    private fun sharePlace() {
        val bookmarkView = bookmarkDetailsView ?: return

        /*
        This section of code builds out a Google Maps URL to trigger driving directions to the bookmarked place.
        Read the documentation at https://developers.google.com/maps/documentation/urls/guide for details about constructing map URLs.
        There are two different styles of URLs to use depending on whether a place ID is available.
        If the user creates an ad-hoc bookmark, then the directions go directly to the latitude/longitude of the bookmark.
        If the bookmark is created from a place, then the directions go to the place based on its ID.
        * */
        var mapUrl = ""
        if (bookmarkView.placeId == null) {
            val location = URLEncoder.encode(
                "${bookmarkView.latitude},"
                        + "${bookmarkView.longitude}", "utf-8"
            )
            mapUrl = "https://www.google.com/maps/dir/?api=1" +
                    "&destination=$location"
        } else {
            val name = URLEncoder.encode(bookmarkView.name, "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1" +
                    "&destination=$name&destination_place_id=" +
                    "${bookmarkView.placeId}"
        }

        /*
        You create the sharing Activity Intent and set the action to ACTION_SEND.
        This tells Android that this Intent is meant to share its data with another application installed on the device.
        * */
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND

        /*
        Multiple types of extra data can be added to the Intent.
        The app that receives the Intent can choose which of the data items to use and which to ignore.
        For example, an email app will use the ACTION_SUBJECT, but a messaging app will likely ignore it.
        There are several other extras available including EXTRA_EMAIL, EXTRA_CC, and EXTRA_BCC.
        * */
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            "Check out ${bookmarkView.name} at:\n$mapUrl"
        )
        sendIntent.putExtra(
            Intent.EXTRA_SUBJECT,
            "Sharing ${bookmarkView.name}"
        )

        /*
        The Intent type is set to a MIME type of “text/plain”.
        This instructs Android that you intend to share plain text data.
        Any app in the system that registers an intent filter for the “text/plain” MIME type will be offered as a choice in the share dialog.
        If you were sharing binary data such as an image, you might use a MIME type of “image/jpeg”.
        * */
        sendIntent.type = "text/plain"

        startActivity(sendIntent)
    }


}