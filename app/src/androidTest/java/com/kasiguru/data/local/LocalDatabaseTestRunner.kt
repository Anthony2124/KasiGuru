package com.kasiguru.data.local

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Database tests do not start app services, authentication or production progress sync. */
class LocalDatabaseTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader,className: String,context: Context): Application =
        super.newApplication(cl,Application::class.java.name,context)
}
