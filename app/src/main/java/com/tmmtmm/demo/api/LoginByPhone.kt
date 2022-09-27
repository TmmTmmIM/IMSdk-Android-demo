package com.tmmtmm.demo.api

import androidx.annotation.Keep
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.annotations.SerializedName
import com.tmmtmm.demo.exception.TmException
import com.tmmtmm.demo.exception.TmmError

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit


@Keep
data class LoginByPhoneRequest(
    var phone: String? = null,
)

@Keep
data class LoginByPhoneResponse(
    @SerializedName("items")
    var items: ItemsDto? = ItemsDto(),

    var status: Int? = 0
)

@Keep
data class ItemsDto(
    @SerializedName("code")
    var code: Int? = 0, // 0
    @SerializedName("expires_in")
    var expiresIn: Int? = 0, // 1626879625
    @SerializedName("id")
    var id: String? = "", // 2x2qlr88wdcz
    @SerializedName("language")
    var language: String? = "",
    @SerializedName("refresh_token")
    var refreshToken: String? = "", // 0B57B8B476532038339F2C82DF9E7081
    @SerializedName("token")
    var token: String? = "" // C32716F237ABFD43D298C276E1F0B30FMngycWxyODh3ZGN6
)

object LoginByPhone {

    fun execute(requestLoginByPhone: LoginByPhoneRequest): ResponseResult<LoginByPhoneResponse?> {
        try {
            val requestBody: RequestBody =
                requestLoginByPhone.toJson().toString().toRequestBody("application/json".toMediaType())
            val req =
                Request.Builder().url("")
                    .post(requestBody)
                    .build()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val response = okHttpClient.newCall(req).execute()

            val type =
                GsonUtils.getType(
                    BaseResponseEntity::class.java,
                    LoginByPhoneResponse::class.java,
                    ItemsDto::class.java
                )
            val responseData: BaseResponseEntity<LoginByPhoneResponse>? =
                response.body?.string()?.responseToEntity(type)

            val result = if (responseData?.isSuccess() == false) {
                //error
                ResponseResult.Failure(TmException(TmmError.ERROR_COMMON))
            } else {
                //success
                ResponseResult.Success(responseData?.data)
            }

            if (result !is ResponseResult.Success) {
                return result
            }

            val status = result.value?.status
            if (status != null && status != 0) {
                return ResponseResult.Failure(TmException(status, ""))
            }

            return result
        } catch (e: Exception){
            e.printStackTrace()
            return ResponseResult.Failure(TmException(500, ""))
        }

    }
}

