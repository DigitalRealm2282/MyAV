package com.digitalrealm.shellsec.utils

interface DefaultPrefrences {
    //region biometric auth
    val isBiometricAuthEnabled: Boolean
    fun updateBiometricStatus(status: Boolean)
    //endregion

    //region notification exclusion
    val isVigilanteExcludedFromNotifications: Boolean
    fun setExcludeVigilanteFromNotificationsStatus(newValue: Boolean)
    //endregion

    //region history deletion
    fun scheduleDeletionHistory()
    fun cancelDeletionHistory()
    //endregion
}