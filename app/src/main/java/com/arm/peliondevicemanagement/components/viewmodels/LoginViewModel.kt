/*
 * Copyright 2020 ARM Ltd.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.peliondevicemanagement.components.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arm.peliondevicemanagement.AppController
import com.arm.peliondevicemanagement.components.models.user.AccountProfile
import com.arm.peliondevicemanagement.components.models.user.UserProfile
import com.arm.peliondevicemanagement.constants.BrandingTheme
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.services.data.BrandingImageResponse
import com.arm.peliondevicemanagement.services.data.CaptchaResponse
import com.arm.peliondevicemanagement.services.repository.CloudRepository
import com.arm.peliondevicemanagement.services.data.ErrorResponse
import com.arm.peliondevicemanagement.services.data.LoginResponse
import com.arm.peliondevicemanagement.utils.PlatformUtils.parseErrorResponseFromJson
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LoginViewModel : ViewModel() {

    companion object {
        private val TAG: String = LoginViewModel::class.java.simpleName
    }

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO

    private val scope = CoroutineScope(coroutineContext)
    private val cloudRepository: CloudRepository = AppController.getCloudRepository()

    private val _userAccountLiveData = MutableLiveData<LoginResponse>()
    private val _userCaptchaLiveData = MutableLiveData<CaptchaResponse>()
    private val _userProfileLiveData = MutableLiveData<UserProfile>()
    private val _accountProfileLiveData = MutableLiveData<AccountProfile>()
    private val _accountBrandingImagesLiveData = MutableLiveData<BrandingImageResponse>()
    private val _errorResponseLiveData = MutableLiveData<ErrorResponse>()

    // LiveData APIs
    fun getLoginActionLiveData(): LiveData<LoginResponse> = _userAccountLiveData
    fun getCaptchaLiveData(): LiveData<CaptchaResponse> = _userCaptchaLiveData
    fun getUserProfileLiveData(): LiveData<UserProfile> = _userProfileLiveData
    fun getAccountProfileLiveData(): LiveData<AccountProfile> = _accountProfileLiveData
    fun getAccountBrandingImagesLiveData(): LiveData<BrandingImageResponse> = _accountBrandingImagesLiveData
    fun getErrorResponseLiveData(): LiveData<ErrorResponse> = _errorResponseLiveData

    // GET & POST APIs
    fun doLogin(username: String, password: String, accountID: String = "") {
        scope.launch {
            try {
                val userAccountResponse = cloudRepository.doAuth(username, password, accountID)
                _userAccountLiveData.postValue(userAccountResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun doImpersonate(accountID: String) {
        scope.launch {
            try {
                val userAccountResponse = cloudRepository.doImpersonate(accountID)
                _userAccountLiveData.postValue(userAccountResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun do2AuthLogin(username: String, password: String,
                     otpCode: String?, captchaID: String?,
                     captchaCode: String?, accountID: String = "") {

        scope.launch {
            try {

                val userAccountResponse: LoginResponse? = if(otpCode != null && captchaID == null){
                    cloudRepository
                        .do2AuthWithOTP(username, password, otpCode, accountID)
                } else if(otpCode == null && captchaID != null){
                    cloudRepository
                        .do2AuthWithCaptcha(username, password, captchaID, captchaCode!!, accountID)
                } else {
                    cloudRepository
                        .do2AuthWithOTPAndCaptcha(username, password, otpCode!!, captchaID!!, captchaCode!!, accountID)
                }

                _userAccountLiveData.postValue(userAccountResponse)
            } catch (e: Throwable){
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun fetchCaptcha() {
        scope.launch {
            try {
                val captchaResponse = cloudRepository.getCaptcha()
                _userCaptchaLiveData.postValue(captchaResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun fetchUserProfile() {
        scope.launch {
            try {
                val userProfileResponse = cloudRepository.getUserProfile()
                _userProfileLiveData.postValue(userProfileResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun fetchAccountProfile() {
        scope.launch {
            try {
                val userAccountProfileResponse = cloudRepository.getAccountProfile()
                _accountProfileLiveData.postValue(userAccountProfileResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun fetchAccountBrandingImages(accountID: String, theme: BrandingTheme) {
        scope.launch {
            try {
                val accountBrandingImagesResponse =
                    cloudRepository.getBrandingImages(accountID, theme)
                _accountBrandingImagesLiveData.postValue(accountBrandingImagesResponse)
            } catch (e: Throwable) {
                LogHelper.debug(TAG, "Exception occurred: ${e.message}")
                val errorResponse = parseErrorResponseFromJson(e.message!!)
                _errorResponseLiveData.postValue(errorResponse)
            }
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()

}