package com.youtubeapis

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

class LoadingDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        // Root container with fixed size (100dp x 100dp)
        val sizeInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 100f, context.resources.displayMetrics
        ).toInt()

        val container = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(sizeInPx, sizeInPx)
            minimumWidth = sizeInPx
            minimumHeight = sizeInPx
        }

        // ProgressBar centered in FrameLayout
        val progressBar = ProgressBar(context).apply {
            isIndeterminate = true
        }

        val progressParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )

        container.addView(progressBar, progressParams)

        val dialog = Dialog(context)
        dialog.setContentView(container)
        dialog.setCancelable(false)

        return dialog
    }

    override fun onStart() {
        super.onStart()
        val sizeInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics
        ).toInt()
        dialog?.window?.setLayout(sizeInPx, sizeInPx)
    }

    companion object {
        private var dialogInstance: LoadingDialogFragment? = null

        fun show(fragmentManager: FragmentManager) {
            if (dialogInstance?.isAdded != true) {
                dialogInstance = LoadingDialogFragment()
                dialogInstance!!.show(fragmentManager, "loading_dialog")
            }
        }

        fun dismiss() {
            dialogInstance?.dismissAllowingStateLoss()
            dialogInstance = null
        }

        fun isShowing(): Boolean {
            return dialogInstance?.isAdded == true
        }
    }
}

