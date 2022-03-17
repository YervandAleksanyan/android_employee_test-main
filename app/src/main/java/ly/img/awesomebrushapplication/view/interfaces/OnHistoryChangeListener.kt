package ly.img.awesomebrushapplication.view.interfaces

import ly.img.awesomebrushapplication.view.data.Brush

interface OnHistoryChangeListener {
    fun onChange(param: Brush.Params)
}