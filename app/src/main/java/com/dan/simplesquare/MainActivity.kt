package com.dan.simplesquare

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.dan.simplesquare.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class MainActivity :
    AppCompatActivity(),
    SeekBar.OnSeekBarChangeListener
{
    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        const val REQUEST_PERMISSIONS = 1
        const val INTENT_OPEN_IMAGE = 2

        const val IMG_WORK_HEIGHT = 1080
    }

    private lateinit var binding: ActivityMainBinding
    private var srcImage: Bitmap? = null
    private var srcName: String = ""
    private var menuSave: MenuItem? = null
    private lateinit var settings: Settings
    private lateinit var rendererScript: RenderScript
    private lateinit var rendererScriptBlur: ScriptIntrinsicBlur
    private var initialUri: Uri? = null

    private var backgroundColor: Int
            get() = (binding.buttonBackgroundColor.background as ColorDrawable).color
            set(color) { binding.buttonBackgroundColor.setBackgroundColor(color) }

    private var borderColor: Int
        get() = (binding.buttonBorderColor.background as ColorDrawable).color
        set(color) { binding.buttonBorderColor.setBackgroundColor(color) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (null != intent && null != intent.action) {
            if (Intent.ACTION_SEND == intent.action) {
                val extraStream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (null != extraStream) {
                    initialUri = extraStream as Uri
                }
            } else if(Intent.ACTION_VIEW == intent.action){
                initialUri = intent.data
            }
        }

        if (!askPermissions())
            onPermissionsAllowed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> handleRequestPermissions(grantResults)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val menuSave = menu.findItem(R.id.save)
        this.menuSave = menuSave

        menuSave.isEnabled = null != this.srcImage

        if (null != initialUri) {
            menu.findItem(R.id.open).isVisible = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                exitApp()
                return true
            }

            R.id.open -> {
                openImage()
                return true
            }

            R.id.save -> {
                saveImage()
                return false
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (INTENT_OPEN_IMAGE == requestCode) {
                if (RESULT_OK == resultCode && null != intent) {
                    intent.data?.let { uri -> loadImage(uri) }
                }
            return
        }
    }

    private fun saveImage() {
        BusyDialog.show(supportFragmentManager)

        GlobalScope.launch(Dispatchers.IO) {
            var fileName = "${srcName}.jpeg"
            var success = false

            try {
                var file = File(Settings.SAVE_FOLDER + "/" + fileName)
                var counter = 0
                while (file.exists() && counter < 998) {
                    counter++
                    fileName = srcName + "_%03d".format(counter) + ".jpeg"
                    file = File(Settings.SAVE_FOLDER + "/" + fileName)
                }

                file.parentFile?.mkdirs()

                val sizeText = binding.spinnerSaveSize.selectedItem as String
                var targetSize = 0
                try {
                    targetSize = sizeText.toInt()
                } catch (e: Exception) {
                }

                val bitmap = generateImage(targetSize)
                if (null != bitmap) {
                    val outputStream = file.outputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, Settings.SAVE_QUALITY, outputStream)
                    outputStream.close()

                    success = true
                    saveSettings()

                    val values = ContentValues()
                    @Suppress("DEPRECATION")
                    values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                }
            } catch (e: Exception) {
            }

            runOnUiThread {
                BusyDialog.dismiss()
                if (success) {
                    Toast.makeText(applicationContext, getString(R.string.save_ok_msg) + fileName, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, getString(R.string.save_failed_msg), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun adjustContrast(colorMatrix: ColorMatrix, value: Float) {
        val scale = 1f + value
        val translate = (-.5f * scale + .5f) * 255f

        val mat = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )

        colorMatrix.postConcat(ColorMatrix(mat))
    }

    private fun adjustBrightness(colorMatrix: ColorMatrix, value: Float) {
        val mat = floatArrayOf(
            1f, 0f, 0f, 0f, value,
            0f, 1f, 0f, 0f, value,
            0f, 0f, 1f, 0f, value,
            0f, 0f, 0f, 1f, 0f
        )

        colorMatrix.postConcat(ColorMatrix(mat))
    }

    private fun adjustSaturation(colorMatrix: ColorMatrix, value: Float) {
        val x = 1 + if (value > 0) 3 * value / 100 else value / 100
        val lumR = 0.3086f
        val lumG = 0.6094f
        val lumB = 0.0820f
        val mat = floatArrayOf(
            lumR * (1 - x) + x, lumG * (1 - x), lumB * (1 - x), 0f, 0f,
            lumR * (1 - x), lumG * (1 - x) + x, lumB * (1 - x), 0f, 0f,
            lumR * (1 - x), lumG * (1 - x), lumB * (1 - x) + x, 0f, 0f,
            0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f
        )

        colorMatrix.postConcat(ColorMatrix(mat))
    }

    private fun getBestImgSize(destWidth: Int, destHeight: Int, imgWidth: Int, imgHeight: Int): Pair<Int, Int> {
        val bestImgHeight = destWidth * imgHeight / imgWidth
        if (bestImgHeight <= destHeight) return Pair(destWidth, bestImgHeight)
        return Pair(destHeight * imgWidth / imgHeight, destHeight)
    }

    private fun getCentredRect(destWidth: Int, destHeight: Int, rectWidth: Int, rectHeight: Int): Rect {
        val left = (destWidth - rectWidth) / 2
        val top = (destHeight - rectHeight) / 2
        return Rect( left, top, left + rectWidth, top + rectHeight);
    }

    private fun generateImage(targetHeight: Int): Bitmap? {
        val srcImage = this.srcImage ?: return null
        val srcImageWidth = srcImage.width
        val srcImageHeight = srcImage.height
        if (srcImageWidth <= 0 || srcImageHeight <= 0) return null

        val targetWidth = when(settings.shape) {
            Settings.SHAPE_4x5 -> (((targetHeight * 4 / 5) + 3) / 4) * 4
            else -> targetHeight
        }

        val destImage = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destImage)

        if (binding.rbBackgroundBlur.isChecked) {
            val scaledBitmap = Bitmap.createScaledBitmap(
                srcImage,
                srcImageWidth / 8,
                srcImageHeight / 8,
                true
            )
            val inputRSBitmap = Allocation.createFromBitmap(rendererScript, scaledBitmap)
            val outputRSBitmap = Allocation.createTyped(rendererScript, inputRSBitmap.type)

            rendererScriptBlur.setInput(inputRSBitmap)
            rendererScriptBlur.forEach(outputRSBitmap)
            outputRSBitmap.copyTo(scaledBitmap)

            val paint = Paint()
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.color = Color.argb(128, 255, 255, 255)

            var scaledWidth = targetWidth
            var scaledHeight = targetWidth * srcImageHeight / srcImageWidth

            if (scaledHeight < targetHeight) {
                scaledHeight = targetHeight
                scaledWidth = targetHeight * srcImageWidth / srcImageHeight
            }

            canvas.drawBitmap(
                scaledBitmap, null,
                Rect((targetWidth - scaledWidth) / 2, (targetHeight - scaledHeight), scaledWidth, scaledHeight),
                paint
            )
        } else {
            canvas.drawColor(backgroundColor)
        }

        val ratio = targetHeight.toFloat() / IMG_WORK_HEIGHT
        val margin = (binding.seekBarMargin.progress * ratio).toInt()
        val border = (binding.seekBarBorder.progress * ratio).toInt()

        val bestSize = getBestImgSize( targetWidth, targetHeight, srcImageWidth, srcImageHeight )

        Log.i("SIMPLE_SQUARE", "Dest: $targetWidth x $targetHeight, Img: $srcImageWidth x $srcImageHeight, BestFit: ${bestSize.first} x ${bestSize.second}")

        var imgWidth = bestSize.first - 2 * margin
        var imgHeight = bestSize.second - 2 * margin

        if (binding.checkBorderShadow.isChecked) {
            val shadow = (10 * ratio).toInt()

            val blurPaint = Paint()
            blurPaint.maskFilter = BlurMaskFilter(16 * ratio, BlurMaskFilter.Blur.NORMAL)
            blurPaint.style = Paint.Style.FILL
            blurPaint.color = Color.argb(160, 0, 0, 0)

            canvas.drawRect(
                getCentredRect( targetWidth, targetHeight, imgWidth, imgHeight ),
                blurPaint
            )

            imgWidth -= shadow
            imgHeight -= shadow
        }

        if (border > 0) {
            val borderPaint = Paint()
            borderPaint.style = Paint.Style.FILL
            borderPaint.color = borderColor
            canvas.drawRect(
                getCentredRect( targetWidth, targetHeight, imgWidth, imgHeight ),
                borderPaint
            )

            imgWidth -= border
            imgHeight -= border
        }

        val colorMatrix = ColorMatrix()

        if (100 != binding.seekBarContrast.progress)
            adjustContrast(colorMatrix, (binding.seekBarContrast.progress - 100) / 200f)

        if (100 != binding.seekBarBrightness.progress)
            adjustBrightness(colorMatrix, binding.seekBarBrightness.progress - 100f)

        if (100 != binding.seekBarSaturation.progress)
            adjustSaturation(colorMatrix, (binding.seekBarSaturation.progress - 100f) / 2)

        val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        filterPaint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        canvas.drawBitmap(
            srcImage,
            null,
            getCentredRect( targetWidth, targetHeight, imgWidth, imgHeight ),
            filterPaint
        )

        return destImage
    }

    private fun loadImageFromUri(uri: Uri): Bitmap? {
        val name = DocumentFile.fromSingleUri(applicationContext, uri)?.name ?: return null
        var bitmap: Bitmap? = null

        try {
            var inputStream = contentResolver.openInputStream(uri) ?: return null
            bitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            inputStream = contentResolver.openInputStream(uri) ?: return null
            val exif = ExifInterface(inputStream)

            val rotate =
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }

            if (rotate != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotate.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
                if (rotatedBitmap != null) bitmap = rotatedBitmap
            }

        } catch (e: Exception) {
        }

        if(null != bitmap) {
            val dotIndex = name.lastIndexOf('.')
            srcName = if (dotIndex >= 0) {
                name.substring(0, dotIndex)
            } else {
                name
            }
        }

        return bitmap
    }

    private fun updateImage() {
        val destImage = generateImage(IMG_WORK_HEIGHT) ?: return
        binding.imageView.setImageBitmap(destImage)
    }

    private fun loadImage(uri: Uri) {
        BusyDialog.show(supportFragmentManager)

        GlobalScope.launch(Dispatchers.IO) {
            val srcImageNew = loadImageFromUri(uri)

            runOnUiThread {
                srcImage = srcImageNew
                if (null == srcImage) {
                    menuSave?.isEnabled = false
                } else {
                    menuSave?.isEnabled = true
                    updateImage()
                }
                BusyDialog.dismiss()
            }
        }
    }

    private fun openImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .putExtra("android.content.extra.SHOW_ADVANCED", true)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setType("image/*")
        startActivityForResult(intent, INTENT_OPEN_IMAGE)
    }

    private fun exitApp() {
        setResult(0)
        finish()
    }

    private fun updateValues() {
        binding.txtBorderValue.text = binding.seekBarBorder.progress.toString()
        binding.txtMarginValue.text = binding.seekBarMargin.progress.toString()
        binding.txtContrastValue.text = (binding.seekBarContrast.progress - 100).toString()
        binding.txtBrightnessValue.text = (binding.seekBarBrightness.progress - 100).toString()
        binding.txtSaturationValue.text = (binding.seekBarSaturation.progress - 100).toString()
    }

    private fun updateShape() {
        val shapeStr = when(settings.shape) {
            Settings.SHAPE_4x5 -> "4:5"
            else -> "1:1"
        }

        val set = ConstraintSet()
        set.clone(binding.frameMainLayout)
        set.setDimensionRatio(binding.frameLayout.id, shapeStr)
        set.applyTo(binding.frameMainLayout)

        binding.frameMainLayout.invalidate()

        updateImage()
    }

    private fun askPermissions(): Boolean {
        for (permission in PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS)
                return true
            }
        }

        return false
    }

    private fun handleRequestPermissions(grantResults: IntArray) {
        var allowedAll = grantResults.size >= PERMISSIONS.size

        if (grantResults.size >= PERMISSIONS.size) {
            for ( result in grantResults ) {
                if (result != PackageManager.PERMISSION_GRANTED ) {
                    allowedAll = false
                    break
                }
            }
        }

        if( allowedAll ) onPermissionsAllowed()
        else exitApp()
    }

    private fun selectColor(forBackgroundColor: Boolean) {
        ColorDialog.show(supportFragmentManager, if (forBackgroundColor) backgroundColor else borderColor) { color ->
            if (forBackgroundColor) {
                backgroundColor = color
            } else {
                borderColor = color
            }
            updateImage()
        }
    }

    private fun saveSettings() {
        settings.contrast = binding.seekBarContrast.progress
        settings.brightness = binding.seekBarBrightness.progress
        settings.saturation = binding.seekBarSaturation.progress
        settings.border = binding.seekBarBorder.progress
        settings.margin = binding.seekBarMargin.progress
        settings.backgroundColor = backgroundColor
        settings.borderColor = borderColor
        settings.borderShadow = binding.checkBorderShadow.isChecked
        settings.backgroundType =
            if (binding.rbBackgroundBlur.isChecked) Settings.BACKGROUND_TYPE_BLUR
            else Settings.BACKGROUND_TYPE_COLOR
        settings.saveSize = binding.spinnerSaveSize.selectedItemPosition

        settings.save()
    }

    private fun onPermissionsAllowed() {
        BusyDialog.create(this)
        settings = Settings(this)
        rendererScript = RenderScript.create(this)
        rendererScriptBlur = ScriptIntrinsicBlur.create(
            rendererScript,
            Element.U8_4(rendererScript)
        )
        rendererScriptBlur.setRadius(8f)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.imageView.setOnClickListener { openImage() }

        binding.seekBarBorder.setOnSeekBarChangeListener(this)
        binding.seekBarMargin.setOnSeekBarChangeListener(this)
        binding.seekBarContrast.setOnSeekBarChangeListener(this)
        binding.seekBarBrightness.setOnSeekBarChangeListener(this)
        binding.seekBarSaturation.setOnSeekBarChangeListener(this)

        binding.txtContrast.setOnLongClickListener {
            binding.seekBarContrast.progress = Settings.DEFAULT_CONTRAST
            true
        }

        binding.txtBrightness.setOnLongClickListener {
            binding.seekBarBrightness.progress = Settings.DEFAULT_BRIGHTNESS
            true
        }

        binding.txtSaturation.setOnLongClickListener {
            binding.seekBarSaturation.progress = Settings.DEFAULT_SATURATION
            true
        }

        binding.txtMargin.setOnLongClickListener {
            binding.seekBarMargin.progress = Settings.DEFAULT_MARGIN
            true
        }

        binding.txtBorder.setOnLongClickListener {
            binding.seekBarBorder.progress = Settings.DEFAULT_BORDER
            true
        }

        binding.buttonBackgroundColor.setOnClickListener { selectColor(true) }
        binding.buttonBorderColor.setOnClickListener {selectColor(false) }

        binding.checkBorderShadow.setOnCheckedChangeListener { _, _ -> updateImage()  }
        binding.rgBackgroundType.setOnCheckedChangeListener { _, _ -> updateImage()  }

        binding.seekBarContrast.progress = settings.contrast
        binding.seekBarBrightness.progress = settings.brightness
        binding.seekBarSaturation.progress = settings.saturation
        binding.seekBarBorder.progress = settings.border
        binding.seekBarMargin.progress = settings.margin
        backgroundColor = settings.backgroundColor
        borderColor = settings.borderColor
        binding.checkBorderShadow.isChecked = settings.borderShadow

        when(settings.backgroundType) {
            Settings.BACKGROUND_TYPE_BLUR -> binding.rgBackgroundType.check(binding.rbBackgroundBlur.id)
            else -> binding.rgBackgroundType.check(binding.rbBackgroundColor.id)
        }

        binding.spinnerSaveSize.setSelection(settings.saveSize)

        binding.spinnerShape.setSelection(settings.shape)

        binding.spinnerShape.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                settings.shape = position
                updateShape()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        updateShape()
        updateValues()

        setContentView(binding.root)

        val initialUri = this.initialUri
        if (null != initialUri)
            loadImage(initialUri)
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        updateValues()
        updateImage()
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }
}