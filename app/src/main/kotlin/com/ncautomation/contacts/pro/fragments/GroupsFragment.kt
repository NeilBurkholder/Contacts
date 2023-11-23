package com.ncautomation.contacts.pro.fragments

import android.content.Context
import android.util.AttributeSet
import com.ncautomation.commons.helpers.TAB_GROUPS
import com.ncautomation.contacts.pro.activities.MainActivity
import com.ncautomation.contacts.pro.activities.SimpleActivity
import com.ncautomation.contacts.pro.databinding.FragmentGroupsBinding
import com.ncautomation.contacts.pro.databinding.FragmentLayoutBinding
import com.ncautomation.contacts.pro.dialogs.CreateNewGroupDialog

class GroupsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.FragmentLayout>(context, attributeSet) {

    private lateinit var binding: FragmentGroupsBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentGroupsBinding.bind(this)
        innerBinding = FragmentLayout(FragmentLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        finishActMode()
        showNewGroupsDialog()
    }

    override fun placeholderClicked() {
        showNewGroupsDialog()
    }

    private fun showNewGroupsDialog() {
        CreateNewGroupDialog(activity as SimpleActivity) {
            (activity as? MainActivity)?.refreshContacts(TAB_GROUPS)
        }
    }
}
