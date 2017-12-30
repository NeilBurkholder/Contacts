package com.simplemobiletools.contacts.helpers

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.SparseArray
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Email
import com.simplemobiletools.contacts.models.Event
import com.simplemobiletools.contacts.models.PhoneNumber
import com.simplemobiletools.contacts.overloads.times
import java.io.ByteArrayOutputStream
import java.util.*

class ContactsHelper(val activity: BaseSimpleActivity) {
    fun getContacts(callback: (ArrayList<Contact>) -> Unit) {
        val contacts = SparseArray<Contact>()
        Thread {
            val sources = activity.config.displayContactSources
            val questionMarks = ("?," * sources.size).trimEnd(',')
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = getContactProjection()
            var selection = "${ContactsContract.Data.MIMETYPE} = ?"
            var selectionArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            if (!activity.config.showAllContacts()) {
                selection += " AND ${ContactsContract.RawContacts.ACCOUNT_NAME} IN ($questionMarks)"
                selectionArgs += sources.toTypedArray()
            }
            val sortOrder = getSortString()

            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                        val firstName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                        val middleName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                        val surname = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                        if (firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty())
                            continue

                        val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.PHOTO_URI) ?: ""
                        val number = ArrayList<PhoneNumber>()       // proper value is obtained below
                        val emails = ArrayList<Email>()
                        val events = ArrayList<Event>()
                        val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME)
                        val contact = Contact(id, firstName, middleName, surname, photoUri, number, emails, events, accountName)
                        contacts.put(id, contact)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            } finally {
                cursor?.close()
            }

            if (activity.config.showPhoneNumbers) {
                val phoneNumbers = getPhoneNumbers()
                val size = phoneNumbers.size()
                for (i in 0 until size) {
                    val key = phoneNumbers.keyAt(i)
                    contacts[key]?.phoneNumbers = phoneNumbers.valueAt(i)
                }
            }

            val contactsSize = contacts.size()
            val resultContacts = ArrayList<Contact>(contactsSize)
            (0 until contactsSize).mapTo(resultContacts) { contacts.valueAt(it) }
            callback(resultContacts)
        }.start()
    }

    private fun getPhoneNumbers(contactId: Int? = null): SparseArray<ArrayList<PhoneNumber>> {
        val phoneNumbers = SparseArray<ArrayList<PhoneNumber>>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
        )

        val selection = if (contactId == null) null else "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) null else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val number = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Phone.TYPE)

                    if (phoneNumbers[id] == null) {
                        phoneNumbers.put(id, ArrayList())
                    }

                    phoneNumbers[id].add(PhoneNumber(number, type))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return phoneNumbers
    }

    private fun getEmails(contactId: Int): SparseArray<ArrayList<Email>> {
        val emails = SparseArray<ArrayList<Email>>()
        val uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.TYPE
        )

        val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val email = cursor.getStringValue(ContactsContract.CommonDataKinds.Email.DATA)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Email.TYPE)

                    if (emails[contactId] == null) {
                        emails.put(contactId, ArrayList())
                    }

                    emails[contactId]!!.add(Email(email, type))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        return emails
    }

    private fun getEvents(contactId: Int): SparseArray<ArrayList<Event>> {
        val events = SparseArray<ArrayList<Event>>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.TYPE
        )
        val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val startDate = cursor.getStringValue(ContactsContract.CommonDataKinds.Event.START_DATE)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Event.TYPE)

                    if (events[contactId] == null) {
                        events.put(contactId, ArrayList())
                    }

                    events[contactId]!!.add(Event(startDate, type))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return events
    }

    fun getContactWithId(id: Int): Contact? {
        if (id == 0) {
            return null
        }

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = getContactProjection()
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id.toString())
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val firstName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                val middleName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                val surname = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.PHOTO_URI) ?: ""
                val number = getPhoneNumbers(id)[id] ?: ArrayList()
                val emails = getEmails(id)[id] ?: ArrayList()
                val events = getEvents(id)[id] ?: ArrayList()
                val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME)
                return Contact(id, firstName, middleName, surname, photoUri, number, emails, events, accountName)
            }
        } finally {
            cursor?.close()
        }

        return null
    }

    fun getContactSources(callback: (ArrayList<String>) -> Unit) {
        val accounts = HashSet<String>()
        Thread {
            val uri = ContactsContract.RawContacts.CONTENT_URI
            val projection = arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME)

            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val name = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME)
                        accounts.add(name)
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor?.close()
            }

            callback(ArrayList(accounts))
        }.start()
    }

    fun getContactSourceType(accountName: String): String {
        val uri = ContactsContract.RawContacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE)
        val selection = "${ContactsContract.RawContacts.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(accountName)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_TYPE)
            }
        } finally {
            cursor?.close()
        }
        return ""
    }

    private fun getContactProjection() = arrayOf(
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.PHOTO_URI,
            ContactsContract.RawContacts.ACCOUNT_NAME
    )

    private fun getSortString(): String {
        val sorting = activity.config.sorting
        var sort = when {
            sorting and SORT_BY_FIRST_NAME != 0 -> "${ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME} COLLATE NOCASE"
            sorting and SORT_BY_MIDDLE_NAME != 0 -> "${ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME} COLLATE NOCASE"
            sorting and SORT_BY_SURNAME != 0 -> "${ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME} COLLATE NOCASE"
            else -> ContactsContract.CommonDataKinds.Phone.NUMBER
        }

        if (sorting and SORT_DESCENDING != 0) {
            sort += " DESC"
        }

        return sort
    }

    fun updateContact(contact: Contact, photoUpdateStatus: Int): Boolean {
        return try {
            activity.toast(R.string.updating)
            val operations = ArrayList<ContentProviderOperation>()
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val selectionArgs = arrayOf(contact.id.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
                operations.add(build())
            }

            // delete phone numbers
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add phone numbers
            contact.phoneNumbers.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, it.value)
                    withValue(ContactsContract.CommonDataKinds.Phone.TYPE, it.type)
                    operations.add(build())
                }
            }

            // delete emails
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add emails
            contact.emails.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    withValue(ContactsContract.CommonDataKinds.Email.DATA, it.value)
                    withValue(ContactsContract.CommonDataKinds.Email.TYPE, it.type)
                    operations.add(build())
                }
            }

            // delete events
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add events
            contact.events.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    withValue(ContactsContract.CommonDataKinds.Event.START_DATE, it.value)
                    withValue(ContactsContract.CommonDataKinds.Event.TYPE, it.type)
                    operations.add(build())
                }
            }

            // photo
            when (photoUpdateStatus) {
                PHOTO_ADDED, PHOTO_CHANGED -> addPhoto(contact, operations)
                PHOTO_REMOVED -> removePhoto(contact, operations)
            }

            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true
        } catch (e: Exception) {
            activity.showErrorToast(e)
            false
        }
    }

    private fun addPhoto(contact: Contact, operations: ArrayList<ContentProviderOperation>): ArrayList<ContentProviderOperation> {
        if (contact.photoUri.isNotEmpty()) {
            val photoUri = Uri.parse(contact.photoUri)
            val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, photoUri)

            val thumbnailSize = getThumbnailSize()
            val scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, false)
            val scaledSizePhotoData = bitmapToByteArray(scaledPhoto)
            scaledPhoto.recycle()

            val fullSizePhotoData = bitmapToByteArray(bitmap)
            bitmap.recycle()

            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, scaledSizePhotoData)
                operations.add(build())
            }

            addFullSizePhoto(contact.id.toLong(), fullSizePhotoData)
        }
        return operations
    }

    private fun removePhoto(contact: Contact, operations: ArrayList<ContentProviderOperation>): ArrayList<ContentProviderOperation> {
        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(contact.id.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            withSelection(selection, selectionArgs)
            operations.add(build())
        }

        return operations
    }

    fun insertContact(contact: Contact): Boolean {
        return try {
            activity.toast(R.string.inserting)
            val operations = ArrayList<ContentProviderOperation>()
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).apply {
                withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.source)
                withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, getContactSourceType(contact.source))
                operations.add(build())
            }

            // names
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
                operations.add(build())
            }

            // phone numbers
            contact.phoneNumbers.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, it.value)
                    withValue(ContactsContract.CommonDataKinds.Phone.TYPE, it.type)
                    operations.add(build())
                }
            }

            // emails
            contact.emails.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    withValue(ContactsContract.CommonDataKinds.Email.DATA, it.value)
                    withValue(ContactsContract.CommonDataKinds.Email.TYPE, it.type)
                    operations.add(build())
                }
            }

            // events
            contact.events.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    withValue(ContactsContract.CommonDataKinds.Event.START_DATE, it.value)
                    withValue(ContactsContract.CommonDataKinds.Event.TYPE, it.type)
                    operations.add(build())
                }
            }

            // photo (inspired by https://gist.github.com/slightfoot/5985900)
            var fullSizePhotoData: ByteArray? = null
            var scaledSizePhotoData: ByteArray?
            if (contact.photoUri.isNotEmpty()) {
                val photoUri = Uri.parse(contact.photoUri)
                val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, photoUri)

                val thumbnailSize = getThumbnailSize()
                val scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, false)
                scaledSizePhotoData = bitmapToByteArray(scaledPhoto)
                scaledPhoto.recycle()

                fullSizePhotoData = bitmapToByteArray(bitmap)
                bitmap.recycle()

                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, scaledSizePhotoData)
                    operations.add(build())
                }
            }

            val results: Array<ContentProviderResult>
            try {
                results = activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            } finally {
                scaledSizePhotoData = null
            }

            // fullsize photo
            if (contact.photoUri.isNotEmpty() && fullSizePhotoData != null) {
                val rawContactId = ContentUris.parseId(results[0].uri)
                addFullSizePhoto(rawContactId, fullSizePhotoData)
            }

            true
        } catch (e: Exception) {
            activity.showErrorToast(e)
            false
        }
    }

    private fun addFullSizePhoto(contactId: Long, fullSizePhotoData: ByteArray) {
        val baseUri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, contactId)
        val displayPhotoUri = Uri.withAppendedPath(baseUri, ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY)
        val fileDescriptor = activity.contentResolver.openAssetFileDescriptor(displayPhotoUri, "rw")
        val photoStream = fileDescriptor.createOutputStream()
        photoStream.write(fullSizePhotoData)
        photoStream.close()
        fileDescriptor.close()
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        var baos: ByteArrayOutputStream? = null
        try {
            baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            return baos.toByteArray()
        } finally {
            baos?.close()
        }
    }

    fun deleteContact(contact: Contact) = deleteContacts(arrayListOf(contact))

    fun deleteContacts(contacts: ArrayList<Contact>) {
        try {
            val operations = ArrayList<ContentProviderOperation>()
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
            contacts.forEach {
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                    val selectionArgs = arrayOf(it.id.toString())
                    withSelection(selection, selectionArgs)
                    operations.add(this.build())
                }
            }

            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    private fun getThumbnailSize(): Int {
        val uri = ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI
        val projection = arrayOf(ContactsContract.DisplayPhoto.THUMBNAIL_MAX_DIM)
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, null, null, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getIntValue(ContactsContract.DisplayPhoto.THUMBNAIL_MAX_DIM)
            }
        } finally {
            cursor?.close()
        }
        return 0
    }
}
