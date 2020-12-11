package com.dan.simplesquare

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.dan.simplesquare.databinding.ColorDialogBinding

class ColorDialog( private val initialColor: Int, private val listener: (Int)->Unit ):
    DialogFragment(),
    SeekBar.OnSeekBarChangeListener
{

    companion object {
        private const val FRAGMENT_TAG = "color"

        fun show(supportFragmentManager: FragmentManager, initialColor: Int, listener: (Int)->Unit) {
            with (ColorDialog( initialColor, listener ) ) {
                isCancelable = false
                show(supportFragmentManager, FRAGMENT_TAG)
            }
        }
    }

    private lateinit var binding: ColorDialogBinding

    private val currentColor: Int
        get() = Color.rgb( binding.seekBarRed.progress, binding.seekBarGreen.progress, binding.seekBarBlue.progress )

    private fun updateColor() {
        val color = currentColor
        binding.txtColor.setBackgroundColor( color )
        binding.txtRed.text = Color.red(color).toString()
        binding.txtGreen.text = Color.green(color).toString()
        binding.txtBlue.text = Color.blue(color).toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ColorDialogBinding.inflate( inflater )

        binding.seekBarRed.progress = Color.red( initialColor )
        binding.seekBarGreen.progress = Color.green( initialColor )
        binding.seekBarBlue.progress = Color.blue( initialColor )

        binding.seekBarRed.setOnSeekBarChangeListener(this)
        binding.seekBarGreen.setOnSeekBarChangeListener(this)
        binding.seekBarBlue.setOnSeekBarChangeListener(this)

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSelect.setOnClickListener {
            listener.invoke( currentColor )
            dismiss()
        }

        updateColor()

        return binding.root
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        updateColor()
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }
}
