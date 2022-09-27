package com.tmmtmm.sdk.logic

import androidx.annotation.WorkerThread
import com.tmmtmm.sdk.core.db.DataBaseManager
import com.tmmtmm.sdk.core.net.manager.NetDbManager
import com.tmmtmm.sdk.cache.LoginCache
import com.tmmtmm.sdk.core.utils.TransferThreadPool
import com.tmmtmm.sdk.db.UserDBManager
import com.tmmtmm.sdk.db.model.UserLinkModel

/**
 * @description
 * @version
 */
class TmLoginLogic private constructor() {
    companion object {

        private var instance: TmLoginLogic? = null


        @JvmName("getInstance")
        fun getInstance(): TmLoginLogic {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = TmLoginLogic()
                    }
                }
            }
            return instance!!
        }
    }


    fun getUserId(): String{
        return LoginCache.getUserId()
    }

    fun setUserId(uid: String){
        LoginCache.setUserId(uid)
    }

    fun isLogin(): Boolean {
        return getUserId().isNotBlank()
    }


    fun login(aUid: String, callBack: LoginCallBack) {


        try {
            TransferThreadPool.submitTask {
                val uid = ""
                setUserId(uid)

                val userModel = UserLinkModel()
                userModel.aUid = aUid
                userModel.uid = uid
                UserDBManager.getInstance().insertUserLink(userModel)

                callBack.success()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callBack.fail(500, "")
        }


    }

    fun logout(aUid: String, callBack: LoginOutCallBack) {

        setUserId("")
        NetDbManager.getInstance().clearAll()
        DataBaseManager.getInstance().close()

        callBack.success()

    }

    interface LoginCallBack {
        fun success()
        fun fail(code: Int, errorMsg: String)
    }

    interface LoginOutCallBack {
        fun success()
        fun fail(code: Int, errorMsg: String)
    }
}