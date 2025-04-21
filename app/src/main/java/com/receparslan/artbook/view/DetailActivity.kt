package com.receparslan.artbook.view

import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.receparslan.artbook.R
import com.receparslan.artbook.databinding.ActivityDetailBinding
import com.receparslan.artbook.model.Art

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding // ViewBinding

    private val art = Art() // Art object

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        art.id = intent.getIntExtra("artID", -1)

        try {
            val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)
            lateinit var cursor: Cursor
            art.id?.let { cursor = database.rawQuery("SELECT * FROM arts WHERE id = ? ", arrayOf(art.id.toString())) }

            val artNameIdx = cursor.getColumnIndex("name")
            val artistNameIdx = cursor.getColumnIndex("artist")
            val dateIdx = cursor.getColumnIndex("date")
            val imageIdx = cursor.getColumnIndex("image")

            if (cursor.moveToNext()) {
                art.name = cursor.getString(artNameIdx)
                art.artistName = cursor.getString(artistNameIdx)
                art.date = cursor.getString(dateIdx)
                art.image = BitmapFactory.decodeByteArray(cursor.getBlob(imageIdx), 0, cursor.getBlob(imageIdx).size)
            }

            cursor.close()
            database.close()
        } catch (e: Exception) {
            e.message?.let { Log.e("Error", it) }
        }

        binding.artNameTextView.text = art.name
        binding.artistTextView.text = art.artistName
        binding.artTimeTextView.text = art.date
        binding.artImageView.setImageBitmap(art.image)
        binding.editButton.setOnClickListener { edit() }
        binding.deleteButton.setOnClickListener { delete() }
    }

    private fun edit() {
        val intent = Intent(this, AddingActivity::class.java)
        intent.putExtra("key", "edit")
        intent.putExtra("artID", art.id)
        startActivity(intent)
    }

    private fun delete() {
        val confirmDialog = AlertDialog.Builder(this)
        confirmDialog.setMessage("Are you sure want to delete?")
        confirmDialog.setPositiveButton("Yes") { _, _ ->
            val query = "DELETE FROM arts WHERE id = ?"

            try {
                val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)
                val statement = database.compileStatement(query)

                art.id?.let { statement.bindLong(1, it.toLong()) }
                statement.execute()

                statement.close()
                database.close()
            } catch (e: Exception) {
                e.message?.let { Log.e("Error", it) }
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        confirmDialog.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        confirmDialog.show()
    }
}