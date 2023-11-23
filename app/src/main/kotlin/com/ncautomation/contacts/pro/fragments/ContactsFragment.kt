package com.ncautomation.contacts.pro.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.ncautomation.commons.extensions.areSystemAnimationsEnabled
import com.ncautomation.commons.extensions.hideKeyboard
import com.ncautomation.commons.models.contacts.Contact
import com.ncautomation.contacts.pro.activities.EditContactActivity
import com.ncautomation.contacts.pro.activities.InsertOrEditContactActivity
import com.ncautomation.contacts.pro.activities.MainActivity
import com.ncautomation.contacts.pro.activities.SimpleActivity
import com.ncautomation.contacts.pro.adapters.ContactsAdapter
import com.ncautomation.contacts.pro.databinding.FragmentContactsBinding
import com.ncautomation.contacts.pro.databinding.FragmentLettersLayoutBinding
import com.ncautomation.contacts.pro.extensions.config
import com.ncautomation.contacts.pro.helpers.LOCATION_CONTACTS_TAB
import com.ncautomation.contacts.pro.interfaces.RefreshContactsListener

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.LetterLayout>(context, attributeSet) {

    private lateinit var binding: FragmentContactsBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentContactsBinding.bind(this)
        innerBinding = LetterLayout(FragmentLettersLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        activity?.hideKeyboard()
        Intent(context, EditContactActivity::class.java).apply {
            context.startActivity(this)
        }
    }

    override fun placeholderClicked() {
        if (activity is MainActivity) {
            (activity as MainActivity).showFilterDialog()
        } else if (activity is InsertOrEditContactActivity) {
            (activity as InsertOrEditContactActivity).showFilterDialog()
        }
    }

    fun setupContactsAdapter(contacts: List<Contact>) {
        setupViewVisibility(contacts.isNotEmpty())
        val currAdapter = innerBinding.fragmentList.adapter

        if (currAdapter == null || forceListRedraw) {
            forceListRedraw = false
            val location = LOCATION_CONTACTS_TAB

            ContactsAdapter(
                activity = activity as SimpleActivity,
                contactItems = contacts.toMutableList(),
                refreshListener = activity as RefreshContactsListener,
                location = location,
                removeListener = null,
                recyclerView = innerBinding.fragmentList,
                enableDrag = false,
            ) {
                (activity as RefreshContactsListener).contactClicked(it as Contact)
            }.apply {
                innerBinding.fragmentList.adapter = this
            }

            if (context.areSystemAnimationsEnabled) {
                innerBinding.fragmentList.scheduleLayoutAnimation()
            }
        } else {
            (currAdapter as ContactsAdapter).apply {
                startNameWithSurname = context.config.startNameWithSurname
                showPhoneNumbers = context.config.showPhoneNumbers
                showContactThumbnails = context.config.showContactThumbnails
                updateItems(contacts)
            }
        }
    }
}
