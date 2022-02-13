package ru.geekbrains.android2.mapapp.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import ru.geekbrains.android2.mapapp.R
import ru.geekbrains.android2.mapapp.model.MarkerObj

class MarkersAdapter(
    private val list: ArrayList<MarkerObj>,
    private var onItemViewClickListener: MarkersFragment.OnItemViewClickListener
) : RecyclerView.Adapter<MarkersAdapter.MarkerHolder>() {
    var clickListenerToEdit = MutableLiveData<MarkerObj>()
    var onDeleteListener = MutableLiveData<MarkerObj>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.marker_item, parent, false)
        return MarkerHolder(view)
    }

    override fun onBindViewHolder(holder: MarkerHolder, position: Int) {
        val obj = list[position]
        holder.title!!.text = obj.title
        holder.description!!.text = obj.description

        holder.edit!!.setOnClickListener { v: View? ->
            clickListenerToEdit.postValue(obj)
        }

        holder.delete!!.setOnClickListener { v: View? ->
            onDeleteListener.postValue(obj)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MarkerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
        var description: TextView? = null
        var edit: ImageView? = null
        var delete: ImageView? = null

        init {
            title = itemView.findViewById(R.id.title)
            description = itemView.findViewById(R.id.description)
            edit = itemView.findViewById(R.id.edit)
            delete = itemView.findViewById(R.id.delete)
            itemView.setOnClickListener {
                onItemViewClickListener?.onItemViewClick(title!!.text.toString())
            }
        }
    }
}