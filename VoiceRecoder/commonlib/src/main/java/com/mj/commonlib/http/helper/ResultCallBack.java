package com.mj.commonlib.http.helper;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by liuwei on 7/3/17.
 */

public abstract class ResultCallBack {
    //成功的回传
   public abstract void  onSuccess( Response response);

    //失败的回传
    public abstract void onFailure(IOException failstr);

}
