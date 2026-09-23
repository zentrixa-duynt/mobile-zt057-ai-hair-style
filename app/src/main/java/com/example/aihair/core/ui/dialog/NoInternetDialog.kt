package com.example.aihair.core.ui.dialog

import android.content.Context
import com.example.aihair.core.ui.base.BaseDialog
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.LayoutDialogNoInternetBinding

class NoInternetDialog(
    context: Context,
    private val onGoToSettings: () -> Unit,
    private val onCancelCallback: () -> Unit
) : BaseDialog(context) {

    private lateinit var binding: LayoutDialogNoInternetBinding

    override fun setupView() {
        binding = LayoutDialogNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        binding.btnCancel.setDebouncedClickListener {
            dismiss()
            onCancelCallback()
        }

        binding.btnGoToSettings.setDebouncedClickListener {
            dismiss()
            onGoToSettings()
        }
    }
}

