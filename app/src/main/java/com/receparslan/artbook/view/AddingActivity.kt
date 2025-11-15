package com.receparslan.artbook.view

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.receparslan.artbook.R
import com.receparslan.artbook.databinding.ActivityAddingBinding
import com.receparslan.artbook.model.Art
import java.io.ByteArrayOutputStream

class AddingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddingBinding // ViewBinding

    private val art = Art() // Art object

    private lateinit var key: String // Key to check if the activity is in edit mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent.getStringExtra("key")?.let { key = it } // Get the key from the intent

        // Set the onClickListeners for the buttons
        binding.saveButton.setOnClickListener { save() }
        binding.artImageView.setOnClickListener { selectImage() }

        // Check if the activity is in edit mode
        if (key == "edit") {
            // Fetch data from SQLite database
            try {
                val db =
                    this.openOrCreateDatabase("Arts", MODE_PRIVATE, null) // Create or open database

                // Create table if not exists
                db.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, name VARCHAR, artist VARCHAR, date VARCHAR, image BLOB)")

                // Cursor to fetch data from database
                val cursor = db.rawQuery("SELECT * FROM arts", null)

                // Get column indexes
                val artId = cursor.getColumnIndex("id")
                val artNameIdx = cursor.getColumnIndex("name")
                val artistNameIdx = cursor.getColumnIndex("artist")
                val dateIdx = cursor.getColumnIndex("date")
                val imageIdx = cursor.getColumnIndex("image")

                // Fetch data from cursor
                if (cursor.moveToNext()) {
                    art.id = cursor.getInt(artId)
                    art.name = cursor.getString(artNameIdx)
                    art.artistName = cursor.getString(artistNameIdx)
                    art.date = cursor.getString(dateIdx)
                    art.image = BitmapFactory.decodeByteArray(
                        cursor.getBlob(imageIdx),
                        0,
                        cursor.getBlob(imageIdx).size
                    )

                    binding.artImageView.setImageBitmap(art.image)
                    binding.artNameEditText.setText(art.name)
                    binding.artistEditText.setText(art.artistName)
                    binding.artTimeEditText.setText(art.date)
                }

                // Close cursor and database
                cursor.close()
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // This function is used to save the data to the SQLite database
    private fun save() {
        // Update the values of the art object
        art.name = binding.artNameEditText.text.toString()
        art.artistName = binding.artistEditText.text.toString()
        art.date = binding.artTimeEditText.text.toString()

        // Check if the image is null
        if (art.image == null)
            binding.artImageView.callOnClick()
        else {
            // ByteArrayOutputStream to compress the image
            val compressedImage = ByteArrayOutputStream()

            // Compress the image
            art.image?.let {
                val smallerImage = makeSmallerImage(it)
                smallerImage.compress(Bitmap.CompressFormat.JPEG, 50, compressedImage)
            }

            // Query to insert or update the data
            val query = if (key == "edit")
                "UPDATE  arts SET name = ?, artist = ?, date = ?, image = ? WHERE id = ?"
            else
                "INSERT INTO arts (name, artist, date, image) VALUES (?,?,?,?)"

            // Insert or update the data
            try {
                val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)
                val sqliteStatement = database.compileStatement(query)

                sqliteStatement.bindString(1, art.name)
                sqliteStatement.bindString(2, art.artistName)
                sqliteStatement.bindString(3, art.date)
                sqliteStatement.bindBlob(4, compressedImage.toByteArray())
                art.id?.let { if (key == "edit") sqliteStatement.bindLong(5, it.toLong()) }
                sqliteStatement.execute()

                sqliteStatement.close()
                database.close()
            } catch (e: Exception) {
                e.message?.let { message -> Log.e("Error", message) }
            }

            // Go back to the home page
            val intentToHomePage = Intent(this, MainActivity::class.java)
            intentToHomePage.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intentToHomePage)
            finish()
        }
    }

    // This function is used to select an image from the gallery
    private fun selectImage() {

        // For Android 13 and above, use the Photo Picker API to select an image
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            pickMediaLauncher.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        else // For below Android 13, request permission and open the gallery
            when { // Check the permission status
                // Permission is granted
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    legacyGalleryLauncher.launch(intentToGallery)
                }

                // Show a snackbar to explain why the permission is needed
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    READ_EXTERNAL_STORAGE
                ) -> {
                    Snackbar.make(
                        binding.root.rootView,
                        "Permission needed to access the gallery",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction("Allow") { permissionLauncher.launch(READ_EXTERNAL_STORAGE) }
                        .show()
                }

                // Request the permission
                else -> permissionLauncher.launch(READ_EXTERNAL_STORAGE)
            }
    }

    // Activity result launcher to pick media (for Android 13 and above)
    private val pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                try {
                    // Check the version of the SDK to use the appropriate method to decode the image
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        art.image = ImageDecoder.decodeBitmap(source)
                        binding.artImageView.setImageBitmap(art.image)
                    }
                } catch (e: Exception) {
                    Log.e("PhotoPicker", "Failed to load image.", e)
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    // Activity result launcher to pick image from gallery (for below Android 13)
    private val legacyGalleryLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data // Get the data from the result
                intentFromResult?.let { resultIntent ->
                    val imageUri = resultIntent.data // Get the image URI from the result intent
                    imageUri?.let {
                        try {
                            // Check the version of the SDK to use the appropriate method to decode the image
                            if (Build.VERSION.SDK_INT >= 28) {
                                // Decode the image using ImageDecoder
                                val source = ImageDecoder.createSource(contentResolver, it)
                                art.image = ImageDecoder.decodeBitmap(source)
                                binding.artImageView.setImageBitmap(art.image)
                            } else {
                                // Decode the image using BitmapFactory
                                try {
                                    val inputStream = contentResolver.openInputStream(it)
                                    art.image = BitmapFactory.decodeStream(inputStream)
                                    binding.artImageView.setImageBitmap(art.image)
                                } catch (e: Exception) {
                                    Log.e("PhotoPicker", "Failed to load image.", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Error", e.message.toString())
                        }
                    }
                }
            }
        }

    // Request permission to access the gallery
    private val permissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
        if (isGranted) {
            val intentToGallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            legacyGalleryLauncher.launch(intentToGallery)
        } else
            Toast.makeText(this, "Permission needed to access the gallery", LENGTH_LONG).show()

    }

    // This function is used to make the image smaller
    private fun makeSmallerImage(image: Bitmap): Bitmap {
        val size = 400


        var width = image.width
        var height = image.height
        val ratio = width.toDouble() / height.toDouble()

        // Check the ratio of the image
        if (ratio > 1) {
            // Landscape image
            width = size
            height = (width / ratio).toInt()
        } else {
            // Portrait image
            height = size
            width = (height * ratio).toInt()
        }

        return image.scale(width, height)
    }
}