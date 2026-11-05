package com.agon.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Stub — full form ported in stage 8 (uses clipboard, attachments, image upload). */
@Composable
fun ReportIssueDialog(showId: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإبلاغ عن مشكلة") },
        text = { Text("سيتم تفعيل هذا الخيار قريباً.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("حسناً") } }
    )
}
