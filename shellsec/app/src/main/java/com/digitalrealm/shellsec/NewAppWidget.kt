package com.digitalrealm.shellsec

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class NewAppWidget : AppWidgetProvider() {


    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        val views = RemoteViews(context.packageName, R.layout.new_app_widget)
//        views.setTextViewText(R.id.play, widgetText)

        val pend = PendingIntent.getService(context,0, Intent(context,MainActivity::class.java),FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.play,pend)
//        views.setOnClickPendingIntent(R.id.play, getPendingSelfIntent(context, ACTION_SCAN))
//        views.setOnClickPendingIntent(R.id.widLockBtn, getPendingSelfIntent(context, ACTION_LOCK))
    }

    override fun onEnabled(context: Context) {

    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent!!.action.equals(ACTION_LOCK)) {
//            playSong(context!!)
        } else if(intent.action.equals(ACTION_SCAN)) {
//            playNextSong(context!!)
        }

    }

    companion object{
        private val ACTION_SCAN = "com.digitalrealm.shellsec.action.ACTION_SCAN"
        private val ACTION_LOCK = "com.digitalrealm.shellsec.action.ACTION_LOCK"
    }
}
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {

    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    views.setTextViewText(R.id.play, widgetText)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
