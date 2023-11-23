package com.ncautomation.contacts.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ncautomation.commons.helpers.ensureBackgroundThread
import com.ncautomation.contacts.pro.extensions.checkAndBackupContactsOnBoot

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            context.apply {
                checkAndBackupContactsOnBoot()
            }
        }
    }
}
