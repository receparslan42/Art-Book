package com.receparslan.artbook.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.receparslan.artbook.databinding.RecyclerRowBinding
import com.receparslan.artbook.model.Art
import com.receparslan.artbook.view.DetailActivity

class RecyclerAdapter(private val artArrayList: ArrayList<Art>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    // ViewBinding
    private lateinit var binding: RecyclerRowBinding

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        artArrayList[position].id?.let { holder.artIdTextView.text = "$it" }
        holder.artNameTextView.text = artArrayList[position].name
        holder.itemView.setOnClickListener {
            val intentToDetailActivity = Intent(holder.itemView.context, DetailActivity::class.java)
            intentToDetailActivity.putExtra("artID", artArrayList[position].id)
            holder.itemView.context.startActivity(intentToDetailActivity)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return artArrayList.size
    }

    // Provide a reference to the views for each data item
    class ViewHolder(binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val artIdTextView = binding.artIdTextView
        val artNameTextView = binding.artNameTextView
    }
}