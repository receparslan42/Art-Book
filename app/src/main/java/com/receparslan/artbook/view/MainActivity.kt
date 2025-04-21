package com.receparslan.artbook.view

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.receparslan.artbook.R
import com.receparslan.artbook.adapter.RecyclerAdapter
import com.receparslan.artbook.databinding.ActivityMainBinding
import com.receparslan.artbook.model.Art

class MainActivity : AppCompatActivity() {

    // ViewBinding
    private lateinit var binding: ActivityMainBinding

    // RecyclerView
    private lateinit var recyclerView: RecyclerView

    // ArrayList of Arts
    private var artArraylist = ArrayList<Art>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fetch data from SQLite database
        try {
            val db = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null) // Create or open database

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
            while (cursor.moveToNext()) {
                val art = Art()

                art.id = cursor.getInt(artId)
                art.name = cursor.getString(artNameIdx)
                art.artistName = cursor.getString(artistNameIdx)
                art.date = cursor.getString(dateIdx)
                art.image = BitmapFactory.decodeByteArray(cursor.getBlob(imageIdx), 0, cursor.getBlob(imageIdx).size)

                artArraylist.add(art)
            }

            // Close cursor and database
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // RecyclerView setup
        recyclerView = binding.recyclerView
        recyclerView.adapter = RecyclerAdapter(artArraylist)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
    }

    // Menu inflater
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return true
    }

    // Menu item click listener
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addAnArt -> {
                val intent = Intent(this, AddingActivity::class.java)
                intent.putExtra("key", "add")
                startActivity(intent)
                true
            }

            else -> false
        }
    }
}