package com.nathaniel.tvhome.media

import android.service.notification.NotificationListenerService
import com.nathaniel.tvhome.TvHomeApplication

/**
 * Exists so the user can grant notification access, which Android requires before
 * a launcher may read other apps' media sessions. No notification content is stored.
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        (application as? TvHomeApplication)?.container?.media?.refresh()
    }

    override fun onListenerDisconnected() {
        (application as? TvHomeApplication)?.container?.media?.refresh()
    }
}
