package com.dan.simplesquare

import android.content.Context
import android.graphics.Color

class Settings(private val activity: MainActivity) {

    companion object {
        const val SAVE_FOLDER = "/storage/emulated/0/SimpleSquare"
        const val SAVE_QUALITY = 90

        const val BACKGROUND_TYPE_COLOR = 0
        const val BACKGROUND_TYPE_BLUR = 1

        private const val CONTRAST_KEY = "contrast"
        const val DEFAULT_CONTRAST = 100

        private const val BRIGHTNESS_KEY = "brightness"
        const val DEFAULT_BRIGHTNESS = 100

        private const val SATURATION_KEY = "saturation"
        const val DEFAULT_SATURATION = 100

        private const val MARGIN_KEY = "margin"
        const val DEFAULT_MARGIN = 10

        private const val BORDER_KEY = "border"
        private const val BORDER_COLOR_KEY = "borderColor"
        private const val BORDER_SHADOW_KEY = "borderShadow"
        const val DEFAULT_BORDER = 0
        const val DEFAULT_BORDER_COLOR = Color.BLACK
        const val DEFAULT_BORDER_SHADOW = false

        private const val BACKGROUND_TYPE_KEY = "backgroundType"
        private const val BACKGROUND_COLOR_KEY = "backgroundColor"
        const val DEFAULT_BACKGROUND_TYPE = BACKGROUND_TYPE_COLOR
        const val DEFAULT_BACKGROUND_COLOR = Color.WHITE

        private const val SAVE_SIZE_KEY = "saveSize"
        const val DEFAULT_SAVE_SIZE = 0

        const val SHAPE_1x1 = 0
        const val SHAPE_4x5 = 1
        private const val SHAPE_KEY = "shape"
        const val DEFAULT_SHAPE = SHAPE_1x1
    }

    private var contrast_ = DEFAULT_CONTRAST
    private var brightness_ = DEFAULT_BRIGHTNESS
    private var saturation_ = DEFAULT_SATURATION
    private var margin_ = DEFAULT_MARGIN

    private var border_ = DEFAULT_BORDER
    private var borderColor_ = DEFAULT_BORDER_COLOR
    private var borderShadow_ = DEFAULT_BORDER_SHADOW

    private var backgroundType_ = DEFAULT_BACKGROUND_TYPE
    private var backgroundColor_ = DEFAULT_BACKGROUND_COLOR

    private var saveSize_ = DEFAULT_SAVE_SIZE

    private var shape_ = DEFAULT_SHAPE

    private var dirty = false

    var shape: Int
        get() = shape_
        set(value) {
            if (shape_ == value) return
            shape_ = value
            dirty = true
        }

    var contrast: Int
        get() = contrast_
        set(value) {
            if (contrast_ == value) return
            contrast_ = value
            dirty = true
        }

    var brightness: Int
        get() = brightness_
        set(value) {
            if (brightness_ == value) return
            brightness_ = value
            dirty = true
        }

    var saturation: Int
        get() = saturation_
        set(value) {
            if (saturation_ == value) return
            saturation_ = value
            dirty = true
        }

    var margin: Int
        get() = margin_
        set(value) {
            if (margin_ == value) return
            margin_ = value
            dirty = true
        }

    var border: Int
        get() = border_
        set(value) {
            if (border_ == value) return
            border_ = value
            dirty = true
        }

    var borderColor: Int
        get() = borderColor_
        set(value) {
            if (borderColor_ == value) return
            borderColor_ = value
            dirty = true
        }

    var borderShadow: Boolean
        get() = borderShadow_
        set(value) {
            if (borderShadow_ == value) return
            borderShadow_ = value
            dirty = true
        }

    var backgroundType: Int
        get() = backgroundType_
        set(value) {
            if (backgroundType_ == value) return
            backgroundType_ = value
            dirty = true
        }

    var backgroundColor: Int
        get() = backgroundColor_
        set(value) {
            if (backgroundColor_ == value) return
            backgroundColor_ = value
            dirty = true
        }

    var saveSize: Int
        get() = saveSize_
        set(value) {
            if (saveSize_ == value) return
            saveSize_ = value
            dirty = true
        }

    init {
        val preferences = activity.getPreferences(Context.MODE_PRIVATE)

        //contrast_ = preferences.getInt(CONTRAST_KEY, contrast_)
        //brightness_ = preferences.getInt(BRIGHTNESS_KEY, brightness_)
        //saturation_ = preferences.getInt(BRIGHTNESS_KEY, saturation_)
        margin_ = preferences.getInt(MARGIN_KEY, margin_)

        border_ = preferences.getInt(BORDER_KEY, border_)
        borderColor_ = preferences.getInt(BORDER_COLOR_KEY, borderColor_)
        borderShadow_ = preferences.getBoolean(BORDER_SHADOW_KEY, borderShadow_)

        backgroundType_ = preferences.getInt(BACKGROUND_TYPE_KEY, backgroundType_)
        backgroundColor_ = preferences.getInt(BACKGROUND_COLOR_KEY, backgroundColor_)

        saveSize_ = preferences.getInt(SAVE_SIZE_KEY, saveSize_)

        shape_ = preferences.getInt(SHAPE_KEY, shape_)
    }

    fun save() {
        if (!dirty) return

        val preferences = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = preferences.edit()

        //editor.putInt(CONTRAST_KEY, contrast_)
        //editor.putInt(BRIGHTNESS_KEY, brightness_)
        //editor.putInt(SATURATION_KEY, saturation_)
        editor.putInt(MARGIN_KEY, margin_)

        editor.putInt(BORDER_KEY, border_)
        editor.putInt(BORDER_COLOR_KEY, borderColor_)
        editor.putBoolean(BORDER_SHADOW_KEY, borderShadow_)

        editor.putInt(BACKGROUND_TYPE_KEY, backgroundType_)
        editor.putInt(BACKGROUND_COLOR_KEY, backgroundColor_)

        editor.putInt(SAVE_SIZE_KEY, saveSize_)

        editor.putInt(SHAPE_KEY, shape_)

        editor.apply()

        dirty = false
    }
}