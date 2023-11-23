package com.ncautomation.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.ncautomation.commons.activities.BaseSimpleActivity
import com.ncautomation.commons.extensions.*
import com.ncautomation.commons.helpers.ContactsHelper
import com.ncautomation.commons.helpers.ensureBackgroundThread
import com.ncautomation.commons.models.contacts.Group
import com.ncautomation.contacts.pro.databinding.DialogRenameGroupBinding

class RenameGroupDialog(val activity: BaseSimpleActivity, val group: Group, val callback: () -> Unit) {
    init {
        val binding = DialogRenameGroupBinding.inflate(activity.layoutInflater).apply {
            renameGroupTitle.setText(group.title)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.ncautomation.commons.R.string.ok, null)
            .setNegativeButton(com.ncautomation.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, com.ncautomation.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameGroupTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.renameGroupTitle.value
                        if (newTitle.isEmpty()) {
                            activity.toast(com.ncautomation.commons.R.string.empty_name)
                            return@setOnClickListener
                        }

                        if (!newTitle.isAValidFilename()) {
                            activity.toast(com.ncautomation.commons.R.string.invalid_name)
                            return@setOnClickListener
                        }

                        group.title = newTitle
                        group.contactsCount = 0
                        ensureBackgroundThread {
                            if (group.isPrivateSecretGroup()) {
                                activity.groupsDB.insertOrUpdate(group)
                            } else {
                                ContactsHelper(activity).renameGroup(group)
                            }
                            activity.runOnUiThread {
                                callback()
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }
}
