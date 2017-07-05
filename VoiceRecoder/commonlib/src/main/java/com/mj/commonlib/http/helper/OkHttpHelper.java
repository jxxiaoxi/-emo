package com.mj.commonlib.http.helper;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by liuwei on 7/1/17.
 */

public class OkHttpHelper {
    public static final OkHttpHelper mOkHttpHelper = new OkHttpHelper();
    private static OkHttpClient mClient;

    /**
     * 　获取实列
     */
    public static OkHttpHelper getOkHttpInstance(Context context) {
        return mOkHttpHelper;

    }

    private OkHttpHelper() {
        initOkHttp();
    }

    private static void initOkHttp() {
        mClient = new OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.SECONDS)
                .writeTimeout(10000, TimeUnit.SECONDS)
                .readTimeout(20000, TimeUnit.SECONDS)
                .build();
    }

    public void uploadFiles(String url, RequestBody requestBody, final ResultCallBack callBack) {
        final Request request = new Request.Builder().url(url).post(requestBody).build();//创建Request
        try {
            //enqueue 异步，execute同步
            mClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("mijie", " postDataToServer onFailure "+e.toString());
                    callBack.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.e("mijie", " postDataToServer onSuccess");
                    callBack.onSuccess(response);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 下载文件
     */
    public void downLoadFiles(String url, final ResultCallBack callBack) {

        Request request = new Request.Builder().url(url).build();
        //获取响应体
        try {
            Log.e("mijie", " downLoadFiles");
            mClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callBack.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    callBack.onSuccess(response);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
