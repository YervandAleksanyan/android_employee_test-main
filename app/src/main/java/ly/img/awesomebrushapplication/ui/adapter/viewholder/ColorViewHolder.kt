package ly.img.awesomebrushapplication.ui.adapter.viewholder

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import ly.img.awesomebrushapplication.R
import ly.img.awesomebrushapplication.data.ColorItem

class ColorViewHolder(private val itemView: View): RecyclerView.ViewHolder(itemView) {
    private var imageView: ImageView? = null
    var onItemClickListener: ((ColorItem) -> Unit)? = null
    init {
        imageView = itemView.findViewById(R.id.colorId)
    }
    fun bind(colorItem: ColorItem) {
        imageView?.setColorFilter(colorItem.color)
        imageView?.setOnClickListener { onItemClickListener?.invoke(colorItem) }
    }
}