package com.dan.simplesquare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.dan.simplesquare.databinding.ActivityMainBinding
import com.pes.androidmaterialcolorpickerdialog.ColorPicker
import java.lang.Integer.max


class MainActivity :
    AppCompatActivity(),
    SeekBar.OnSeekBarChangeListener
{

    companion object {
        val PERMISSIONS = arrayOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        const val REQUEST_PERMISSIONS = 1
        const val INTENT_OPEN_IMAGE = 2

        const val IMG_WORK_SIZE = 1080
    }

    private lateinit var binding: ActivityMainBinding
    private var srcImage: Bitmap? = null
    private lateinit var shadowBitmap: Bitmap
    private var srcName: String = ""
    private lateinit var menuSave: MenuItem
    private lateinit var settings: Settings

    private var backgroundColor: Int
            get() = (binding.buttonBackgroundColor.getBackground() as ColorDrawable).color
            set(color) { binding.buttonBackgroundColor.setBackgroundColor(color) }

    private var borderColor: Int
        get() = (binding.buttonBorderColor.getBackground() as ColorDrawable).color
        set(color) { binding.buttonBorderColor.setBackgroundColor(color) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!askPermissions())
            onPermissionsAllowed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> handleRequestPermissions(requestCode, permissions, grantResults)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menuSave = menu.findItem(R.id.save)
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
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (INTENT_OPEN_IMAGE == requestCode) {
                if (RESULT_OK == resultCode && null != intent) {
                    val uri = intent.data
                    if (null != uri)
                        loadImage(uri)
                }
            return
        }
    }

    private fun adjustContrast(colorMatrix: ColorMatrix, value: Int) {
        if (value == 100) return

        var scale = 1f + value / 200f
        val translate = (-.5f * scale + .5f) * 255f

        val mat = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )

        colorMatrix.postConcat(ColorMatrix(mat))
    }

    private fun adjustSaturation(colorMatrix: ColorMatrix, valueInt: Int) {
        if (valueInt == 100) return

        val value = (valueInt - 100).toFloat()
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

    private fun generateImage(targetSize_: Int): Bitmap? {
        val srcImage = this.srcImage ?: return null
        val srcImageWidth = srcImage.width
        val srcImageHeight = srcImage.height
        if (srcImageWidth <= 0 || srcImageHeight <= 0) return null

        var targetSize =
            if (targetSize_ <= 0) max(srcImageWidth, srcImageHeight)
            else targetSize_

        val ratio = targetSize.toFloat() / IMG_WORK_SIZE
        val margin = (binding.seekBarMargin.progress * ratio).toInt()
        val border = (binding.seekBarBoder.progress * ratio).toInt()
        val fullMargin = margin + border

        if (targetSize_ <= 0) targetSize += 2 * fullMargin

        val destImgSize =
            if (targetSize_ <= 0) max(srcImageWidth, srcImageHeight)
            else targetSize - 2 * fullMargin

        val destImage = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(destImage)
        canvas.drawColor(backgroundColor)

        var destImgWidth: Int
        var destImgHeight: Int

        if (srcImageWidth > srcImageHeight) {
            destImgWidth = destImgSize
            destImgHeight = destImgSize * srcImageHeight / srcImageWidth
        } else {
            destImgHeight = destImgSize
            destImgWidth = destImgSize * srcImageWidth / srcImageHeight
        }

        val destImgX = (targetSize - destImgWidth) / 2
        val destImgY = (targetSize - destImgHeight) / 2

        if (binding.checkBorderShadow.isChecked) {
            val blurPaint = Paint()
            blurPaint.maskFilter = BlurMaskFilter(16 * ratio, BlurMaskFilter.Blur.NORMAL)

            var blurWidth: Int
            var blurHeight: Int
            if (srcImage.width > srcImage.height) {
                blurHeight = targetSize
                blurWidth = targetSize * srcImage.width / srcImage.height
            } else {
                blurWidth = targetSize
                blurHeight = targetSize * srcImage.height / srcImage.width
            }

            var blurX = (targetSize - blurWidth) / 2
            var blurY = (targetSize - blurHeight) / 2

            canvas.drawBitmap(
                srcImage,
                null,
                Rect(blurX, blurY, blurX + blurWidth, blurY + blurHeight),
                blurPaint
            )
        }

        if (border > 0) {
            val borderPaint = Paint()
            borderPaint.style = Paint.Style.FILL
            borderPaint.color = borderColor
            canvas.drawRect(
                (destImgX - border).toFloat(),
                (destImgY - border).toFloat(),
                (destImgX + destImgWidth + border).toFloat(),
                (destImgY + destImgHeight + border).toFloat(),
                borderPaint
            )
        }

        val colorMatrix = ColorMatrix()
        adjustContrast(colorMatrix, binding.seekBarContrast.progress)
        val filterPaint = Paint()
        filterPaint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        canvas.drawBitmap(
            srcImage,
            null,
            Rect(destImgX, destImgY, destImgX + destImgWidth, destImgY + destImgHeight),
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
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }

            if (rotate != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotate.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != null) bitmap = rotatedBitmap
            }

        } catch (e: Exception) {
        }

        if(null != bitmap) {
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex >= 0)
                srcName = name.substring(0, dotIndex)
            else
                srcName = name
        }

        return bitmap
    }

    private fun updateImage() {
        val destImage = generateImage(IMG_WORK_SIZE) ?: return
        binding.imageView.setImageBitmap(destImage)
    }

    private fun loadImage(uri: Uri) {
        val srcImage = loadImageFromUri(uri)
        this.srcImage = srcImage
        if (null == srcImage) {
            menuSave.isEnabled = false
            return
        }

        menuSave.isEnabled = true
        updateImage()
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
        binding.txtBorderValue.text = binding.seekBarBoder.progress.toString()
        binding.txtMarginValue.text = binding.seekBarMargin.progress.toString()
        binding.txtContrastValue.text = (binding.seekBarContrast.progress - 100).toString()
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

    private fun handleRequestPermissions(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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

    private fun selectColor(initColor: Int, l: (Int) -> Unit) {
        val dialog = ColorPicker(
            this, Color.red(initColor), Color.green(initColor), Color.blue(
                initColor
            )
        )
        dialog.enableAutoClose()
        dialog.setCallback { color -> l.invoke(color) }
        dialog.show()
    }

    private fun saveSettings() {
        settings.contrast = binding.seekBarContrast.progress
        settings.border = binding.seekBarBoder.progress
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
        settings = Settings(this)
        shadowBitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.shadow)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.imageView.setOnClickListener { if (null == srcImage) openImage() }

        binding.seekBarBoder.setOnSeekBarChangeListener(this)
        binding.seekBarMargin.setOnSeekBarChangeListener(this)
        binding.seekBarContrast.setOnSeekBarChangeListener(this)

        binding.txtContrast.setOnLongClickListener {
            binding.seekBarContrast.progress = Settings.DEFAULT_CONTRAST
            true
        }

        binding.txtMargin.setOnLongClickListener {
            binding.seekBarMargin.progress = Settings.DEFAULT_MARGIN
            true
        }

        binding.txtBorder.setOnLongClickListener {
            binding.seekBarBoder.progress = Settings.DEFAULT_BORDER
            true
        }

        binding.buttonBackgroundColor.setOnClickListener {
            selectColor(backgroundColor) { color ->
                backgroundColor = color
                updateImage()
            }
        }

        binding.buttonBorderColor.setOnClickListener {
            selectColor(borderColor) { color ->
                borderColor = color
                updateImage()
            }
        }

        binding.seekBarContrast.progress = settings.contrast
        binding.seekBarBoder.progress = settings.border
        binding.seekBarMargin.progress = settings.margin
        backgroundColor = settings.backgroundColor
        borderColor = settings.borderColor
        binding.checkBorderShadow.isChecked = settings.borderShadow

        when(settings.backgroundType) {
            Settings.BACKGROUND_TYPE_BLUR -> binding.rgBackgroundType.check(binding.rbBackgroundBlur.id)
            else -> binding.rgBackgroundType.check(binding.rbBackgroundColor.id)
        }

        binding.spinnerSaveSize.setSelection(settings.saveSize)

        updateValues()

        setContentView(binding.root)
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