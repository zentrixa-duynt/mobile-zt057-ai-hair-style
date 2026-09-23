package com.example.aihair.feature.photo_editor.component

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseDialog
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.widget.ColorWheelView
import com.google.android.material.card.MaterialCardView

class ColorPickerDialog(
    context: Context,
    private val initialHex: String? = null,
    private val onColorSelected: (String) -> Unit
) : BaseDialog(context) {

    private var currentHex: String = initialHex ?: "#7384F9" // Default

    override fun setupView() {
        setContentView(R.layout.dialog_color_picker)
        val colorWheelView = findViewById<ColorWheelView>(R.id.color_wheel_view)
        colorWheelView?.setInitialColor(currentHex)
        val cardSelectedColor = findViewById<MaterialCardView>(R.id.card_selected_color)
        val txtHexCode = findViewById<TextView>(R.id.txt_hex_code)
        val btnClose = findViewById<AppCompatButton>(R.id.btn_dialog_close)
        val btnSelect = findViewById<AppCompatButton>(R.id.btn_dialog_select)

        // Set initial
        cardSelectedColor?.setCardBackgroundColor(Color.parseColor(currentHex))
        txtHexCode?.text = currentHex

        colorWheelView?.onColorChangedListener = { colorInt, hex ->
            currentHex = hex
            cardSelectedColor?.setCardBackgroundColor(colorInt)
            txtHexCode?.text = hex
        }

        btnClose?.setDebouncedClickListener {
            dismiss()
        }

        btnSelect?.setDebouncedClickListener {
            onColorSelected(currentHex)
            dismiss()
        }
    }
}