package com.ncautomation.contacts.pro.dialogs

import com.ncautomation.commons.activities.BaseSimpleActivity
import com.ncautomation.commons.extensions.beGoneIf
import com.ncautomation.commons.extensions.getAlertDialogBuilder
import com.ncautomation.commons.extensions.setupDialogStuff
import com.ncautomation.commons.helpers.*
import com.ncautomation.contacts.pro.R
import com.ncautomation.contacts.pro.databinding.DialogChangeSortingBinding
import com.ncautomation.contacts.pro.extensions.config

class ChangeSortingDialog(val activity: BaseSimpleActivity, private val showCustomSorting: Boolean = false, private val callback: () -> Unit) {
    private var currSorting = 0
    private var config = activity.config
    private val binding = DialogChangeSortingBinding.inflate(activity.layoutInflater)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(com.ncautomation.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(com.ncautomation.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, com.ncautomation.commons.R.string.sort_by)
            }

        currSorting = if (showCustomSorting && config.isCustomOrderSelected) {
            SORT_BY_CUSTOM
        } else {
            config.sorting
        }

        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = binding.sortingDialogRadioSorting

        sortingRadio.setOnCheckedChangeListener { group, checkedId ->
            val isCustomSorting = checkedId == binding.sortingDialogRadioCustom.id
            binding.sortingDialogRadioOrder.beGoneIf(isCustomSorting)
            binding.divider.root.beGoneIf(isCustomSorting)
        }

        val sortBtn = when {
            currSorting and SORT_BY_FIRST_NAME != 0 -> binding.sortingDialogRadioFirstName
            currSorting and SORT_BY_MIDDLE_NAME != 0 -> binding.sortingDialogRadioMiddleName
            currSorting and SORT_BY_SURNAME != 0 -> binding.sortingDialogRadioSurname
            currSorting and SORT_BY_FULL_NAME != 0 -> binding.sortingDialogRadioFullName
            currSorting and SORT_BY_CUSTOM != 0 -> binding.sortingDialogRadioCustom
            else -> binding.sortingDialogRadioDateCreated
        }
        sortBtn.isChecked = true

        if (showCustomSorting) {
            binding.sortingDialogRadioCustom.isChecked = config.isCustomOrderSelected
        }
        binding.sortingDialogRadioCustom.beGoneIf(!showCustomSorting)
    }

    private fun setupOrderRadio() {
        var orderBtn = binding.sortingDialogRadioAscending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = binding.sortingDialogRadioDescending
        }
        orderBtn.isChecked = true
    }

    private fun dialogConfirmed() {
        val sortingRadio = binding.sortingDialogRadioSorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_first_name -> SORT_BY_FIRST_NAME
            R.id.sorting_dialog_radio_middle_name -> SORT_BY_MIDDLE_NAME
            R.id.sorting_dialog_radio_surname -> SORT_BY_SURNAME
            R.id.sorting_dialog_radio_full_name -> SORT_BY_FULL_NAME
            R.id.sorting_dialog_radio_custom -> SORT_BY_CUSTOM
            else -> SORT_BY_DATE_CREATED
        }

        if (sorting != SORT_BY_CUSTOM && binding.sortingDialogRadioOrder.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (showCustomSorting) {
            if (sorting == SORT_BY_CUSTOM) {
                config.isCustomOrderSelected = true
            } else {
                config.isCustomOrderSelected = false
                config.sorting = sorting
            }
        } else {
            config.sorting = sorting
        }

        callback()
    }
}
