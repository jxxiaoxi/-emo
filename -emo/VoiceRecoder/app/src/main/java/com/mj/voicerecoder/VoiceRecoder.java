package com.mj.voicerecoder;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.iflytek.cloud.SpeechUtility;
import com.mj.voicerecoder.db.greendao.DaoMaster;
import com.mj.voicerecoder.db.greendao.DaoSession;
import com.mj.voicerecoder.db.greendao.UserDao;
import com.zhy.http.okhttp.OkHttpUtils;

import org.greenrobot.greendao.database.Database;

import java.util.concurrent.TimeUnit;

import cn.jpush.android.api.JPushInterface;
import okhttp3.OkHttpClient;

/**
 * Created by liuwei on 6/16/17.
 */

public class VoiceRecoder extends Application {
    //讯飞语音开发官网　http://www.xfyun.cn/
    private static final String APPID= "594347bd";//讯飞平台申请的appId
    private DaoSession mDaoSession;
    private static VoiceRecoder mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        initSpeech();
        initOkHttp();
        initJPush();
        //initDb();
    }

    public static VoiceRecoder getInstances(){
        return mContext;
    }


    private void initJPush() {
        JPushInterface.setDebugMode(true);
        JPushInterface.init(this);
    }

    /**
     * 初始化讯飞语音
     */
    private void initSpeech() {
        SpeechUtility.createUtility(VoiceRecoder.this, "appid=" + APPID);
        // 以下语句用于设置日志开关（默认开启），设置成false时关闭语音云SDK日志打印
        // Setting.setShowLog(false);
    }


    /**
     * 初始化OkHttp
     */
    private void initOkHttp() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .addInterceptor(new LoggerInterceptor("TAG"))
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                //其他配置
                .build();

        OkHttpUtils.initClient(okHttpClient);
    }


    /*初始化数据库相关*/
    private void initDb() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "recluse-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        mDaoSession = daoMaster.newSession();
    }

    public DaoSession getDaoSession() {
        return mDaoSession;
    }
}
