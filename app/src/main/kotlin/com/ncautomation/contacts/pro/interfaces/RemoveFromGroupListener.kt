package com.ncautomation.contacts.pro.interfaces

import com.ncautomation.commons.models.contacts.Contact

interface RemoveFromGroupListener {
    fun removeFromGroup(contacts: ArrayList<Contact>)
}
