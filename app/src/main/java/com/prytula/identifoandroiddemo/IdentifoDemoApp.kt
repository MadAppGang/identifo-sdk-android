package com.prytula.identifoandroiddemo

import android.app.Application
import com.prytula.IdentifoAuthentication


/*
 * Created by Eugene Prytula on 2/25/21.
 * Copyright (c) 2021 MadAppGang. All rights reserved.
 */

// Step 2
class IdentifoDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appID = "59fd884d8f6b180001f5b4e2"
        val secret = "vU06QTsJ3dWXiAdJxgdsTPAg"
        val baseUrl = "https://auth.smartrun.app"

        IdentifoAuthentication.initAuthenticator(this, baseUrl, appID, secret)
    }
}