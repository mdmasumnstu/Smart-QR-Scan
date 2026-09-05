package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MyQrProfile(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val address: String = "",
    val socialLink: String = ""
) {
    val name: String get() = fullName

    constructor(name: String, phone: String, email: String, company: String, website: String) : this(
        fullName = name,
        phone = phone,
        email = email,
        company = company,
        website = website
    )

    fun toVCard(): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        if (fullName.isNotBlank()) {
            sb.appendLine("FN:$fullName")
            sb.appendLine("N:$fullName;;;;")
        }
        if (company.isNotBlank()) sb.appendLine("ORG:$company")
        if (jobTitle.isNotBlank()) sb.appendLine("TITLE:$jobTitle")
        if (phone.isNotBlank()) sb.appendLine("TEL;TYPE=CELL:$phone")
        if (email.isNotBlank()) sb.appendLine("EMAIL;TYPE=INTERNET:$email")
        if (website.isNotBlank()) sb.appendLine("URL:$website")
        if (address.isNotBlank()) sb.appendLine("ADR:;;$address;;;;")
        if (socialLink.isNotBlank()) sb.appendLine("NOTE:$socialLink")
        sb.appendLine("END:VCARD")
        return sb.toString().trim()
    }
}

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_qr_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _vibrateOnScan = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE, true))
    val vibrateOnScan: StateFlow<Boolean> = _vibrateOnScan.asStateFlow()

    private val _soundOnScan = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundOnScan: StateFlow<Boolean> = _soundOnScan.asStateFlow()

    private val _autoCopy = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COPY, false))
    val autoCopy: StateFlow<Boolean> = _autoCopy.asStateFlow()

    private val _autoOpenUrls = MutableStateFlow(prefs.getBoolean(KEY_AUTO_OPEN_URLS, false))
    val autoOpenUrls: StateFlow<Boolean> = _autoOpenUrls.asStateFlow()

    private val _saveHistory = MutableStateFlow(prefs.getBoolean(KEY_SAVE_HISTORY, true))
    val saveHistory: StateFlow<Boolean> = _saveHistory.asStateFlow()

    private val _myProfile = MutableStateFlow(loadProfile())
    val myProfile: StateFlow<MyQrProfile> = _myProfile.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setVibrateOnScan(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
        _vibrateOnScan.value = enabled
    }

    fun setSoundOnScan(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _soundOnScan.value = enabled
    }

    fun setAutoCopy(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COPY, enabled).apply()
        _autoCopy.value = enabled
    }

    fun setAutoOpenUrls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_OPEN_URLS, enabled).apply()
        _autoOpenUrls.value = enabled
    }

    fun setSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
        _saveHistory.value = enabled
    }

    fun saveProfile(profile: MyQrProfile) {
        prefs.edit()
            .putString(KEY_PROFILE_NAME, profile.fullName)
            .putString(KEY_PROFILE_PHONE, profile.phone)
            .putString(KEY_PROFILE_EMAIL, profile.email)
            .putString(KEY_PROFILE_WEB, profile.website)
            .putString(KEY_PROFILE_COMPANY, profile.company)
            .putString(KEY_PROFILE_TITLE, profile.jobTitle)
            .putString(KEY_PROFILE_ADDRESS, profile.address)
            .putString(KEY_PROFILE_SOCIAL, profile.socialLink)
            .apply()
        _myProfile.value = profile
    }

    fun saveMyProfile(profile: MyQrProfile) = saveProfile(profile)

    private fun loadProfile(): MyQrProfile {
        return MyQrProfile(
            fullName = prefs.getString(KEY_PROFILE_NAME, "") ?: "",
            phone = prefs.getString(KEY_PROFILE_PHONE, "") ?: "",
            email = prefs.getString(KEY_PROFILE_EMAIL, "") ?: "",
            website = prefs.getString(KEY_PROFILE_WEB, "") ?: "",
            company = prefs.getString(KEY_PROFILE_COMPANY, "") ?: "",
            jobTitle = prefs.getString(KEY_PROFILE_TITLE, "") ?: "",
            address = prefs.getString(KEY_PROFILE_ADDRESS, "") ?: "",
            socialLink = prefs.getString(KEY_PROFILE_SOCIAL, "") ?: ""
        )
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_SOUND = "key_sound"
        private const val KEY_AUTO_COPY = "key_auto_copy"
        private const val KEY_AUTO_OPEN_URLS = "key_auto_open_urls"
        private const val KEY_SAVE_HISTORY = "key_save_history"

        private const val KEY_PROFILE_NAME = "key_profile_name"
        private const val KEY_PROFILE_PHONE = "key_profile_phone"
        private const val KEY_PROFILE_EMAIL = "key_profile_email"
        private const val KEY_PROFILE_WEB = "key_profile_web"
        private const val KEY_PROFILE_COMPANY = "key_profile_company"
        private const val KEY_PROFILE_TITLE = "key_profile_title"
        private const val KEY_PROFILE_ADDRESS = "key_profile_address"
        private const val KEY_PROFILE_SOCIAL = "key_profile_social"
    }
}
