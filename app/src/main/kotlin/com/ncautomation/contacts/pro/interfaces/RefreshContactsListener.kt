package com.ncautomation.contacts.pro.interfaces

import com.ncautomation.commons.models.contacts.Contact

interface RefreshContactsListener {
    fun refreshContacts(refreshTabsMask: Int)

    fun contactClicked(contact: Contact)
}
