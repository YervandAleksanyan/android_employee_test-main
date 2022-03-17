package ly.img.awesomebrushapplication.ui

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.awesomebrushapplication.R
import ly.img.awesomebrushapplication.ui.adapter.ColorAdapter
import ly.img.awesomebrushapplication.view.BrushCanvas
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Math.random
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val colorAdapter = ColorAdapter()
    private var brushCanvas: BrushCanvas? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*

          == Layout ==

            We require a very simple layout here and you can use an XML layout or code to create it:
              * Load Image -> Load an image from the gallery and display it on screen.
              * Save Image -> Save the final image to the gallery.
              * Color -> Let the user select a color from a list of colors.
              * Size -> Let the user specify the radius of a stroke via a slider.
              * Clear all -> Let the user remove all strokes from the image to start over.

          ----------------------------------------------------------------------
         | HINT: The layout doesn't have to look good, but it should be usable. |
          ----------------------------------------------------------------------

          == Requirements ==
              * Your drawing must be applied to the original image, not the downscaled preview. That means that 
                your brush must work in image coordinates instead of view coordinates and the saved image must have 
                the same resolution as the originally loaded image.
              * You can ignore OutOfMemory issues. If you run into memory issues just use a smaller image for testing.

          == Things to consider ==
            These features would be nice to have. Structure your program in such a way, that it could be added afterwards 
            easily. If you have time left, feel free to implement it already.

              * The user can make mistakes, so a history (undo/redo) would be nice.
              * The image usually doesn't change while brushing, but can be replaced with a higher resolution variant. A 
                common scenario would be a small preview but a high-resolution final rendering. Keep this in mind when 
                creating your data structures.
         */
        initView()
        initListeners()
    }

    private fun initView() {
        brushCanvas = findViewById(R.id.drawingLayout)
        val recyclerView = findViewById<RecyclerView>(R.id.colorRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        colorAdapter.onItemClickListener = { onChangeColor(it.color) }
        recyclerView.adapter = colorAdapter
    }

    private fun initListeners() {
        val openGalleryIcon = findViewById<ImageView>(R.id.chooseImageIcon)
        openGalleryIcon.setOnClickListener {
            onPressLoadImage()
        }

        val thicknessGroup = findViewById<Group>(R.id.thicknessGroup)
        val thicknessIcon = findViewById<ImageView>(R.id.thickness)
        thicknessIcon.setOnClickListener { 
            when (thicknessGroup.visibility) {
                View.GONE -> thicknessGroup.visibility = View.VISIBLE
                View.VISIBLE -> thicknessGroup.visibility = View.GONE
                else -> Unit
            }
        }

        val progressLabel = findViewById<TextView>(R.id.thicknessLabel)
        val progressBar = findViewById<SeekBar>(R.id.thicknessProgressBar)
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brushCanvas?.setHardness(progress.toFloat())
                progressLabel.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit

        })

        val undo = findViewById<ImageView>(R.id.undo)
        undo.setOnClickListener { brushCanvas?.undo() }

        val redo = findViewById<ImageView>(R.id.redo)
        redo.setOnClickListener { brushCanvas?.redo() }

        val clearAll = findViewById<ImageView>(R.id.clearAll)
        clearAll.setOnClickListener { brushCanvas?.clearAll() }

        val download = findViewById<ImageView>(R.id.download)
        download.setOnClickListener { onPressSave() }
    }

    private fun onPressLoadImage() {
        val intent = Intent(Intent.ACTION_PICK)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            intent.type = "image/*"
        } else {
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        }
        startActivityForResult(intent, GALLERY_INTENT_RESULT)
    }

    private fun handleGalleryImage(uri: Uri) {
        // Adjust size of the drawable area, after loading the image.
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
        brushCanvas?.initSourceBitmap(bitmap)
    }

    @MainThread
    private fun onPressSave() {
        brushCanvas?.takeIf { it.isDrawingEnabled }?.let {
            findViewById<Group>(R.id.thicknessGroup)?.visibility = View.GONE
            lifecycleScope.launchWhenCreated {
                withContext(Dispatchers.IO) {
                    saveBrushToGallery()
                }
            }
        }
    }

    private fun onChangeColor(@ColorInt color: Int) {
        brushCanvas?.setColor(colorInt = color)
    }

    @WorkerThread
    private fun saveBrushToGallery() {
        brushCanvas?.captureView(window) {
            handleDownloadBitmap(it)
        }
//        val bitmap: Bitmap = brushCanvas?.getCurrentBitmap() ?: return
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && resultCode == Activity.RESULT_OK && requestCode == GALLERY_INTENT_RESULT) {
            val uri = data.data
            if (uri != null) {
                handleGalleryImage(uri)
            }
        }
    }

    private fun handleDownloadBitmap(bitmap: Bitmap) {
        val fileName = "downloaded_image_${Random.nextInt(1000)}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = this.contentResolver
            val contentValues = ContentValues()
            contentValues.apply {
                put(
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    fileName
                )
                put(
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    "image/png"
                )
                put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                )
                put(MediaStore.Images.Media.IS_PENDING, true)
            }
            val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            fileUri?.let {
                val outputStream = contentResolver.openOutputStream(it)
                if (outputStream != null) {
                    try {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                contentValues.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(it, contentValues, null, null)
            }
        } else {
            val target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                fileName
            )
            val outputStream = FileOutputStream(target)
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val contentValues = ContentValues()
            contentValues.apply {
                put(
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    fileName
                )
                put(
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    "image/png"
                )
            }
            contentValues.put(MediaStore.Images.Media.DATA, target.absolutePath)
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }
    }

    companion object {
        const val GALLERY_INTENT_RESULT = 0
    }
}