package com.example.aihair.feature.hair_result.component

import android.content.Context
import android.widget.TextView
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseDialog
import com.example.aihair.core.ui.click.setDebouncedClickListener

class ReportDialog(context: Context) : BaseDialog(context) {

    override fun setupView() {
        setContentView(R.layout.dialog_report)

        val cbSpam = findViewById<android.widget.CheckBox>(R.id.cb_spam)
        val cbInappropriate = findViewById<android.widget.CheckBox>(R.id.cb_inappropriate)
        val cbCopyright = findViewById<android.widget.CheckBox>(R.id.cb_copyright)
        val cbOffensive = findViewById<android.widget.CheckBox>(R.id.cb_offensive)
        val cbOther = findViewById<android.widget.CheckBox>(R.id.cb_other)
        
        val btnCancel = findViewById<TextView>(R.id.btn_cancel)
        val btnReport = findViewById<TextView>(R.id.btn_report)

        // Mặc định chọn tuỳ chọn đầu tiên
        cbSpam.isChecked = true

        btnCancel.setDebouncedClickListener {
            dismiss()
        }

        btnReport.setDebouncedClickListener {
            val hasSelection = cbSpam.isChecked || cbInappropriate.isChecked || cbCopyright.isChecked || cbOffensive.isChecked || cbOther.isChecked
            if (hasSelection) {
                android.widget.Toast.makeText(context, context.getString(R.string.msg_reported_reason), android.widget.Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                android.widget.Toast.makeText(context, context.getString(R.string.msg_please_select_reason), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}