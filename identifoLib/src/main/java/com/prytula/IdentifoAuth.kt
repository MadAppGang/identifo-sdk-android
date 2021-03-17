package com.prytula

import android.content.Context
import com.prytula.identifolib.di.dependenciesModule
import com.prytula.identifolib.entities.*
import com.prytula.identifolib.entities.deanonymize.DeanonimizeDataSet
import com.prytula.identifolib.entities.deanonymize.DeanonimizeResponse
import com.prytula.identifolib.entities.federatedLogin.FederatedLoginDataSet
import com.prytula.identifolib.entities.federatedLogin.FederatedLoginResponse
import com.prytula.identifolib.entities.logging.LoginDataSet
import com.prytula.identifolib.entities.logging.LoginResponse
import com.prytula.identifolib.entities.phoneLogin.PhoneLoginDataSet
import com.prytula.identifolib.entities.phoneLogin.PhoneLoginResponse
import com.prytula.identifolib.entities.register.RegisterDataSet
import com.prytula.identifolib.entities.register.RegisterResponse
import com.prytula.identifolib.entities.requestCode.RequestPhoneCodeDataSet
import com.prytula.identifolib.entities.requestCode.RequestPhoneCodeResponse
import com.prytula.identifolib.entities.reserPassword.ResetPasswordDataSet
import com.prytula.identifolib.entities.reserPassword.ResetPasswordResponse
import com.prytula.identifolib.extensions.Result
import com.prytula.identifolib.extensions.onSuccess
import com.prytula.identifolib.extensions.suspendApiCall
import com.prytula.identifolib.network.QueriesService
import com.prytula.identifolib.storages.ITokenDataStorage
import com.prytula.identifolib.storages.IUserStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin


/*
 * Created by Eugene Prytula on 2/5/21.
 * Copyright (c) 2021 MadAppGang. All rights reserved.
 */

object IdentifoAuth : KoinComponent {
    private val tokenDataStorage by inject<ITokenDataStorage>()
    private val userStorage by inject<IUserStorage>()
    private val queriesService by inject<QueriesService>()
    private val backgroundCoroutineDispatcher by inject<CoroutineDispatcher>()

    private val _authState by lazy { MutableStateFlow(getInitialAuthentificationState()) }
    val authState by lazy { _authState.asStateFlow() }

    fun initAuthenticator(
        context: Context,
        baseUrl: String,
        appId: String,
        secretKey: String
    ) {
        startKoin {
            androidContext(context)
            modules(
                dependenciesModule(
                    appId = appId,
                    appSecret = secretKey,
                    baseUrl = baseUrl
                )
            )
        }
    }

    private fun getInitialAuthentificationState(): AuthState {
        val tokens = tokenDataStorage.getTokens()
        val user = userStorage.getUser()
        val refreshToken = tokens.refresh
        return if (refreshToken.isExpired()) {
            AuthState.Deauthentificated
        } else {
            val accessToken = tokens.access?.jwtEncoded
            AuthState.Authentificated(user, accessToken)
        }
    }

    internal fun saveTokens(
        accessToken: String,
        refreshToken: String,
        user: IdentifoUser? = userStorage.getUser()
    ) {
        tokenDataStorage.setTokens(
            Tokens(
                Token.Access(accessToken),
                Token.Refresh(refreshToken)
            )
        )
        user?.let { userStorage.setUser(it) }
        _authState.value = AuthState.Authentificated(user, accessToken)
    }

    internal fun clearTokens() {
        tokenDataStorage.clearAll()
        userStorage.clearAll()
        _authState.value = AuthState.Deauthentificated
    }

    suspend fun registerWithUsernameAndPassword(
        username: String,
        password: String,
        isAnonymous: Boolean
    ): Result<RegisterResponse, ErrorResponse> = withContext(backgroundCoroutineDispatcher) {
        val registerCredentials =
            RegisterDataSet(username = username, password = password, anonymous = isAnonymous)
        return@withContext suspendApiCall {
            queriesService.registerWithUsernameAndPassword(registerCredentials)
        }.onSuccess {
            val fetchedUser = it.registeredUser
            val identifoUser = IdentifoUser(fetchedUser.id, fetchedUser.username, isAnonymous)
            saveTokens(it.accessToken, it.refreshToken, identifoUser)
        }
    }

    suspend fun deanonymizeUser(
        oldUsername: String,
        oldPassword: String,
        newUsername: String,
        newPassword: String
    ): Result<DeanonimizeResponse, ErrorResponse> = withContext(backgroundCoroutineDispatcher) {
        val deanonimizeDataSet = DeanonimizeDataSet(
            oldUsername,
            oldPassword,
            newUsername,
            newPassword
        )
        return@withContext suspendApiCall { queriesService.deanonymize(deanonimizeDataSet) }
    }

    suspend fun loginWithUsernameAndPassword(
        username: String,
        password: String
    ): Result<LoginResponse, ErrorResponse> = withContext(backgroundCoroutineDispatcher) {
        val loginDataSet = LoginDataSet(username, password)
        return@withContext suspendApiCall {
            queriesService.loginWithUsernameAndPassword(loginDataSet)
        }.onSuccess {
            val fetchedUser = it.loggedUser
            val identifoUser = IdentifoUser(fetchedUser.id, fetchedUser.username, false)
            saveTokens(it.accessToken, it.refreshToken, identifoUser)
        }
    }

    suspend fun requestPhoneCode(phoneNumber: String): Result<RequestPhoneCodeResponse, ErrorResponse> =
        withContext(backgroundCoroutineDispatcher) {
            val requestPhoneCodeDataSet = RequestPhoneCodeDataSet(phoneNumber)
            return@withContext suspendApiCall {
                queriesService.requestPhoneCode(
                    requestPhoneCodeDataSet
                )
            }
        }

    suspend fun resetPassword(email: String): Result<ResetPasswordResponse, ErrorResponse> =
        withContext(backgroundCoroutineDispatcher) {
            val resetPasswordDataSet = ResetPasswordDataSet(email)
            return@withContext suspendApiCall { queriesService.resetPassword(resetPasswordDataSet) }
        }

    suspend fun phoneLogin(
        phoneLogin: String,
        code: String
    ): Result<PhoneLoginResponse, ErrorResponse> = withContext(backgroundCoroutineDispatcher) {
        val loginDataSet = PhoneLoginDataSet(phoneLogin, code)
        return@withContext suspendApiCall { queriesService.phoneLogin(loginDataSet) }.onSuccess {
            val fetchedUser = it.loggedUser
            val identifoUser = IdentifoUser(fetchedUser.id, fetchedUser.username, false)
            saveTokens(it.accessToken, it.refreshToken, identifoUser)
        }
    }

    suspend fun federatedLogin(
        provider: String,
        token: String
    ): Result<FederatedLoginResponse, ErrorResponse> = withContext(backgroundCoroutineDispatcher) {
        val federatedLoginDataSet = FederatedLoginDataSet(provider, token)
        return@withContext suspendApiCall { queriesService.federatedLogin(federatedLoginDataSet) }.onSuccess {
            val fetchedUser = it.loggedUser
            val identifoUser = IdentifoUser(fetchedUser.id, fetchedUser.username, false)
            saveTokens(it.accessToken, it.refreshToken, identifoUser)
        }
    }

    suspend fun logout(): Result<Unit, ErrorResponse> = withContext(backgroundCoroutineDispatcher) {
        return@withContext suspendApiCall { queriesService.logout() }.onSuccess {
            clearTokens()
        }
    }
}
