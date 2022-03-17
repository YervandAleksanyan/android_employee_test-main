package ly.img.awesomebrushapplication.view.data

import android.graphics.Path

data class BrushHistory(
    val path: Path,
    val param: Brush.Params = Brush.Params()
)