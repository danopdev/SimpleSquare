package com.dan.simplesquare

import android.content.Context
import android.graphics.Color

class Settings(val activity: MainActivity) {

    companion object {
        const val BACKGROUND_TYPE_COLOR = 0
        const val BACKGROUND_TYPE_BLUR = 1

        private const val CONTRAST_KEY = "contrast"
        const val DEFAULT_CONTRAST = 100

        private const val MARGIN_KEY = "margin"
        const val DEFAULT_MARGIN = 10

        private const val BORDER_KEY = "border"
        private const val BORDER_COLOR_KEY = "borderColor"
        private const val BORDER_SHADOW_KEY = "borderShadow"
        const val DEFAULT_BORDER = 0
        const val DEFAULT_BORDER_COLOR = Color.BLACK
        const val DEFAULT_BORDER_SHADOW = false

        private const val BACKGROUNG_TYPE_KEY = "backgroundType"
        private const val BACKGROUNG_COLOR_KEY = "backgroundColor"
        const val DEFAULT_BACKGROUND_TYPE = BACKGROUND_TYPE_COLOR
        const val DEFAULT_BACKGROUND_COLOR = Color.WHITE

        private const val SAVE_SIZE_KEY = "saveSize"
        const val DEFAULT_SAVE_SIZE = -1
    }

    private var contrast_ = DEFAULT_CONTRAST
    private var margin_ = DEFAULT_MARGIN

    private var border_ = DEFAULT_BORDER
    private var borderColor_ = DEFAULT_BORDER_COLOR
    private var borderShadow_ = DEFAULT_BORDER_SHADOW

    private var backgroundType_ = DEFAULT_BACKGROUND_TYPE
    private var backgroundColor_ = DEFAULT_BACKGROUND_COLOR

    private var saveSize_ = DEFAULT_SAVE_SIZE

    private var dirty = false

    var contrast: Int
        get() = contrast_
        set(value) {
            if (contrast_ == value) return
            contrast_ = value
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

        contrast_ = preferences.getInt(CONTRAST_KEY, contrast_)
        margin_ = preferences.getInt(MARGIN_KEY, margin_)

        border_ = preferences.getInt(BORDER_KEY, border_)
        borderColor_ = preferences.getInt(BORDER_COLOR_KEY, borderColor_)
        borderShadow_ = preferences.getBoolean(BORDER_SHADOW_KEY, borderShadow_)

        backgroundType_ = preferences.getInt(BACKGROUNG_TYPE_KEY, backgroundType_)
        backgroundColor_ = preferences.getInt(BACKGROUNG_COLOR_KEY, backgroundColor_)

        saveSize_ = preferences.getInt(SAVE_SIZE_KEY, saveSize_)
    }

    fun save() {
        if (!dirty) return

        val preferences = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = preferences.edit()

        editor.putInt(CONTRAST_KEY, contrast_)
        editor.putInt(MARGIN_KEY, margin_)

        editor.putInt(BORDER_KEY, border_)
        editor.putInt(BORDER_COLOR_KEY, borderColor_)
        editor.putBoolean(BORDER_SHADOW_KEY, borderShadow_)

        editor.putInt(BACKGROUNG_TYPE_KEY, backgroundType_)
        editor.putInt(BACKGROUNG_COLOR_KEY, backgroundColor_)

        editor.putInt(SAVE_SIZE_KEY, saveSize_)

        editor.commit()

        dirty = false
    }
}