package ly.img.awesomebrushapplication.view.data

import android.graphics.Color
import android.graphics.Path
import ly.img.awesomebrushapplication.view.interfaces.OnHistoryChangeListener

class Brush {
    /**
     * param for thickness and color
     */
    private val currentParam = Params()

    /**
     * stack for tracking the brush editing step by step
     */
    private var brushHistory = mutableListOf<BrushHistory>()

    /**
     * cursor indicating current history step
     */
    private var currentCursor: Int = -1

    var onHistoryChangedListener: OnHistoryChangeListener? = null

    fun add(path: Path) {
        if (currentCursor > 0 && currentCursor < brushHistory.size - 1) {
            brushHistory = brushHistory.take(currentCursor + 1).toMutableList()
        } else if (currentCursor == -1 && brushHistory.isNotEmpty()) {
            brushHistory.clear()
        }
        val snapshot = BrushHistory(path, currentParam.copy())
        brushHistory.add(snapshot)
        currentCursor++
        onHistoryChangedListener?.onChange(currentParam)
    }

    fun undo() {
        if (!canUndo()) {
            currentCursor--
            onHistoryChangedListener?.onChange(currentParam)
        }
    }

    fun redo() {
        if (!canRedo()) {
            currentCursor++
            onHistoryChangedListener?.onChange(currentParam)
        }
    }

    fun clearAll() {
        currentCursor = -1
        brushHistory.clear()
        onHistoryChangedListener?.onChange(currentParam)
    }

    fun changeColor(color: Int) {
        currentParam.color = color
        onHistoryChangedListener?.onChange(currentParam)
    }

    fun changeHardness(strokeWidth: Float) {
        currentParam.thickness = strokeWidth
        onHistoryChangedListener?.onChange(currentParam)
    }

    fun getCurrentHistory() = if (currentCursor != -1) {
        brushHistory.take(currentCursor + 1)
    } else {
        emptyList()
    }

    /**
     * determine can perform undo action on the history
     * @return true if undo us possible and false otherwise
     */
    private fun canUndo(): Boolean =
        currentCursor == -1 || brushHistory.isEmpty()

    /**
     * determine can perform redo action on the history
     */
    private fun canRedo(): Boolean =
        (currentCursor + 1 > brushHistory.size - 1)

    class Params {
        var color: Int = DEFAULT_COLOR
        var scaleFactor: Int = 1
        var thickness: Float = DEFAULT_THICKNESS
            set(value) {
                field = value
                if (value > MAX_THICKNESS) field = MAX_THICKNESS
                if (value < MIN_THICKNESS) field = MIN_THICKNESS
            }

        fun copy(): Params {
            val copy = Params()
            copy.color = this.color
            copy.thickness = this.thickness
            copy.scaleFactor = this.scaleFactor
            return copy
        }
    }

    companion object {
        private const val DEFAULT_THICKNESS = 12f
        private const val MAX_THICKNESS = 100f
        private const val MIN_THICKNESS = 1f
        private const val DEFAULT_COLOR = Color.BLACK
    }

}