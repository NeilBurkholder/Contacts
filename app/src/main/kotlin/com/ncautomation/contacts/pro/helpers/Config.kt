package com.ncautomation.contacts.pro.helpers

import android.content.Context
import com.ncautomation.commons.helpers.BaseConfig
import com.ncautomation.commons.helpers.SHOW_TABS

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()

    var autoBackupContactSources: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_CONTACT_SOURCES, setOf())!!
        set(autoBackupContactSources) = prefs.edit().remove(AUTO_BACKUP_CONTACT_SOURCES).putStringSet(AUTO_BACKUP_CONTACT_SOURCES, autoBackupContactSources)
            .apply()

}
