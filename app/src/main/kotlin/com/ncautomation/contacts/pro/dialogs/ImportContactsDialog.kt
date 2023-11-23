package com.ncautomation.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.ncautomation.commons.extensions.getAlertDialogBuilder
import com.ncautomation.commons.extensions.getPublicContactSource
import com.ncautomation.commons.extensions.setupDialogStuff
import com.ncautomation.commons.extensions.toast
import com.ncautomation.commons.helpers.ContactsHelper
import com.ncautomation.commons.helpers.SMT_PRIVATE
import com.ncautomation.commons.helpers.ensureBackgroundThread
import com.ncautomation.contacts.pro.R
import com.ncautomation.contacts.pro.activities.SimpleActivity
import com.ncautomation.contacts.pro.databinding.DialogImportContactsBinding
import com.ncautomation.contacts.pro.extensions.config
import com.ncautomation.contacts.pro.extensions.showContactSourcePicker
import com.ncautomation.contacts.pro.helpers.VcfImporter
import com.ncautomation.contacts.pro.helpers.VcfImporter.ImportResult.IMPORT_FAIL

class ImportContactsDialog(val activity: SimpleActivity, val path: String, private val callback: (refreshView: Boolean) -> Unit) {
    private var targetContactSource = ""
    private var ignoreClicks = false

    init {
        val binding = DialogImportContactsBinding.inflate(activity.layoutInflater).apply {
            targetContactSource = activity.config.lastUsedContactSource
            activity.getPublicContactSource(targetContactSource) {
                importContactsTitle.setText(it)
                if (it.isEmpty()) {
                    ContactsHelper(activity).getContactSources {
                        val localSource = it.firstOrNull { it.name == SMT_PRIVATE }
                        if (localSource != null) {
                            targetContactSource = localSource.name
                            activity.runOnUiThread {
                                importContactsTitle.setText(localSource.publicName)
                            }
                        }
                    }
                }
            }

            importContactsTitle.setOnClickListener {
                activity.showContactSourcePicker(targetContactSource) {
                    targetContactSource = if (it == activity.getString(R.string.phone_storage_hidden)) SMT_PRIVATE else it
                    activity.getPublicContactSource(it) {
                        val title = if (it == "") activity.getString(R.string.phone_storage) else it
                        importContactsTitle.setText(title)
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.ncautomation.commons.R.string.ok, null)
            .setNegativeButton(com.ncautomation.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.import_contacts) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        ignoreClicks = true
                        activity.toast(com.ncautomation.commons.R.string.importing)
                        ensureBackgroundThread {
                            val result = VcfImporter(activity).importContacts(path, targetContactSource)
                            handleParseResult(result)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun handleParseResult(result: VcfImporter.ImportResult) {
        activity.toast(
            when (result) {
                VcfImporter.ImportResult.IMPORT_OK -> com.ncautomation.commons.R.string.importing_successful
                VcfImporter.ImportResult.IMPORT_PARTIAL -> com.ncautomation.commons.R.string.importing_some_entries_failed
                else -> com.ncautomation.commons.R.string.importing_failed
            }
        )
        callback(result != IMPORT_FAIL)
    }
}
