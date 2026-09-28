package com.example.aihair.core.ui.dialog

import android.content.Context
import android.content.Intent
import android.provider.Settings

object NoInternetDialogHelper {
    private var currentDialog: NoInternetDialog? = null

    fun show(context: Context, onCancel: () -> Unit = {}) {
        if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return
        if (currentDialog?.isShowing == true) return

        currentDialog = NoInternetDialog(
            context = context,
            onGoToSettings = {
                try {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (e: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (ignored: Exception) {
                    }
                }
                currentDialog = null
            },
            onCancelCallback = {
                onCancel()
                currentDialog = null
            }
        ).apply {
            setOnDismissListener {
                currentDialog = null
            }
        }
        currentDialog?.show()
    }
    
    fun dismiss(context: Context? = null) {
        if (context != null) {
            // Only dismiss if the dialog's context matches the provided context
            val dialogContext = currentDialog?.context
            var baseContext = dialogContext
            if (dialogContext is android.content.ContextWrapper) {
                baseContext = dialogContext.baseContext
            }
            if (baseContext == context || dialogContext == context) {
                currentDialog?.dismiss()
                currentDialog = null
            }
        } else {
            currentDialog?.dismiss()
            currentDialog = null
        }
    }
}

