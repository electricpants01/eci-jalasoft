package eci.technician

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.squareup.picasso.OkHttpDownloader
import com.squareup.picasso.Picasso
import eci.signalr.IConnection
import eci.signalr.messenger.MessengerModule
import eci.technician.data.TechnicianModule
import eci.technician.helpers.AppAuth
import eci.technician.models.gps.UpdatePosition
import io.realm.Realm
import io.realm.RealmConfiguration
import microsoft.aspnet.signalr.client.Platform
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent
import java.util.concurrent.CopyOnWriteArrayList

class MainApplication : Application(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        Platform.loadPlatformComponent(AndroidPlatformComponent())
        val builder = Picasso.Builder(this)
        builder.downloader(OkHttpDownloader(this, Long.MAX_VALUE))
        val picasso = builder.build()
        Picasso.setSingletonInstance(picasso)
        Realm.init(this)
        val configuration = RealmConfiguration.Builder()
            .schemaVersion(4)
            /**
             * Use migration for production
             */
//            .migration(MainMigration())
            .modules(TechnicianModule(), MessengerModule())
            /**
             * Use deleteRealmIfMigrationNeeded
             * for dev
             */
            .deleteRealmIfMigrationNeeded()
            .allowWritesOnUiThread(true)
            .allowQueriesOnUiThread(true)
            .build()

        Realm.setDefaultConfiguration(configuration)
        AppAuth.init(this)
        AppAuth.getInstance().sendGpsStatus()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        isAppOpened = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForeground() {
        isAppOpened = true
    }

    companion object {
        @JvmStatic
        var connection: IConnection? = null
        var lastLocation: Location? = null
        var locations: MutableList<UpdatePosition> = CopyOnWriteArrayList()

        var isAppOpened: Boolean = false
        lateinit var appContext: Context
    }
}