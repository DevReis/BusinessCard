package com.devreis.businesscard

import android.app.Application
import com.devreis.businesscard.data.AppDatabase
import com.devreis.businesscard.data.BusinessCardRepo

class App : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { BusinessCardRepo(database.businessDao()) }
}