package com.kasiguru.data.local

import android.app.Application
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/** Database tests do not start app services, authentication or production progress sync. */
class LocalDatabaseTestRunner : AndroidJUnitRunner() {
    private var originalMessagingState: Int? = null
    private var messagingComponent: ComponentName? = null

    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        // Firebase's init provider can deliver a token to the Hilt service even with a plain
        // Application. Keep that service out of these database-only tests and restore its state.
        val component = ComponentName(context.packageName,
            "com.kasiguru.util.notification.KasiGuruMessagingService")
        messagingComponent = component
        originalMessagingState = context.packageManager.getComponentEnabledSetting(component)
        context.packageManager.setComponentEnabledSetting(component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        return super.newApplication(cl, Application::class.java.name, context)
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        messagingComponent?.let { component ->
            originalMessagingState?.let { state ->
                targetContext.packageManager.setComponentEnabledSetting(component, state,
                    PackageManager.DONT_KILL_APP)
            }
        }
        super.finish(resultCode, results)
    }
}
