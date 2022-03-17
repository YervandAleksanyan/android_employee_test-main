package ly.img.awesomebrushapplication.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ly.img.awesomebrushapplication.R
import ly.img.awesomebrushapplication.data.ColorItem
import ly.img.awesomebrushapplication.ui.adapter.viewholder.ColorViewHolder

class ColorAdapter : RecyclerView.Adapter<ColorViewHolder>() {
    private val data = listOf(
        ColorItem(Color.BLACK),
        ColorItem(Color.WHITE),
        ColorItem(Color.BLUE),
        ColorItem(Color.RED),
        ColorItem(Color.CYAN),
        ColorItem(Color.DKGRAY),
        ColorItem(Color.GREEN),
        ColorItem(Color.LTGRAY),
        ColorItem(Color.MAGENTA),
        ColorItem(Color.YELLOW)
    )
    var onItemClickListener: ((ColorItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.color_item, parent, false)
        return ColorViewHolder(view).apply { onItemClickListener = this@ColorAdapter.onItemClickListener }
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size
}