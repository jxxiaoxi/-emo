package com.mj.voicerecoder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.sunflower.FlowerCollector;
import com.mj.voicerecoder.base.BaseActivity;
import com.mj.voicerecoder.bean.ResultBean;
import com.mj.voicerecoder.constant.Constant;
import com.mj.voicerecoder.db.greendao.DaoSession;
import com.mj.voicerecoder.http.ProgressListener;
import com.mj.voicerecoder.http.ProgressResponseBody;
import com.mj.voicerecoder.media.CameraHelper;
import com.mj.voicerecoder.speech.setting.Settings;
import com.mj.voicerecoder.speech.util.ApkInstaller;
import com.mj.voicerecoder.speech.util.FucUtil;
import com.mj.voicerecoder.speech.util.JsonParser;
import com.mj.voicerecoder.util.AppUtils;
import com.mj.voicerecoder.util.LocationTool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final int DISPLAY_VIEW = 10086;
    private Camera mCamera;
    private TextureView mPreview;
    private ImageView iv_settings;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;
    private ImageView iv_recoder;

    private boolean isRecording = false;
    private static final String TAG = "mijie";
    private Button bt_speech, bt_face_detect;
    private boolean isSpeeding = false;
    private static final int DISPLAY_TYPE = 2000;
    private static final int PREVIEW_HIGTH = 480;
    private static final int PREVIEW_WIGTH = 640;

    //start vioce
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;

    private EditText et_voice_result;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    //private EditText et_voice_result;
    private Toast mToast;
    private SharedPreferences mSharedPreferences;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 语记安装助手类
    ApkInstaller mInstaller;
    //end vioce

    private DaoSession mUserDao;
    private String mPath;

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISPLAY_VIEW:
                    showIVSpeedView();
                    break;
                case DISPLAY_TYPE:
                    showToast();
                    break;
                default:
                    break;
            }
        }
    };


    public void showToast(){
        Toast.makeText(this,"视频上传成功",Toast.LENGTH_SHORT).show();
    }

    /**
     *
     */
    private void showIVSpeedView() {
        iv_recoder.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_settings = (ImageView) findViewById(R.id.iv_settings);
        iv_settings.setOnClickListener(this);
        bt_face_detect = (Button) findViewById(R.id.bt_face_detect);
        bt_face_detect.setOnClickListener(this);
        checkoutPermissions();//获取权限

        //AppUtils.getPhoneMacAddress(this);
        //Log.e("mijie", "mac---" + AppUtils.getWifiMacAddress(this));
        // AppUtils.getLocation(this);
        // getData();
    }

    private void gotoApp() {
        new Thread() {
            @Override
            public void run() {
                //postData();
                //testProgess();
                //postDataToServer();
            }
        }.start();


        initCameraView();//初始化camera view
        initVoiceView();//初始voice view
        //获取位置
        ArrayList<String> locations = LocationTool.getInstance().getLocationData(this);
        if (locations != null) {
            for (String str : locations) {
                Log.e("mijie", "xxxxxxx" + str);
            }
        }

        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        mInstaller = new ApkInstaller(this);
        mIatDialog = new RecognizerDialog(this, mInitListener);
        mSharedPreferences = getSharedPreferences(Settings.PREFER_NAME,
                Activity.MODE_PRIVATE);

        boolean isAutoIatShow = mSharedPreferences.getBoolean(
                getString(R.string.pref_key_auto_iat_show), false);
        Log.e(TAG, "isAutoIatShow : " + isAutoIatShow);
        //是否设置了自动语音听写
        if (isAutoIatShow) {
            collectVioceing();
        }

        bt_speech.setOnClickListener(this);
    }


    private void testProgess() {
        Log.e("mijie", "testProgess ");

//        File file = new File("/storage/emulated/0/voicerecoder/CameraSample/VID_20170628_095258.mp4");
//        RequestBody fileBody = RequestBody.create(MediaType.parse("video/3gp"), file);
//        RequestBody requestBody = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("file", "test.mp4", fileBody)
//                .addFormDataPart("mac","adfasdfadff12bbb")
//                .addFormDataPart("longitude","121.393303")
//                .addFormDataPart("latitude","31.121527")
//                .addFormDataPart("description","test")
//                .build();
//        Request request = new Request.Builder()
//                .url(Constant.BASE_URI)
//                .post(requestBody)
//                .build();

        Request request = new Request.Builder()
                .url("https://publicobject.com/helloworld.txt")
                .build();
        final ProgressListener progressListener = new ProgressListener() {

            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                Log.e("mijie", "bytesRead :  " + bytesRead);
                Log.e("mijie", "contentLength :  " + contentLength);
                Log.e("mijie", "done :  " + done);
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(1000, TimeUnit.SECONDS)
                .writeTimeout(1000, TimeUnit.SECONDS)
                .readTimeout(3000, TimeUnit.SECONDS)
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        Log.e("mijie", "addNetworkInterceptor ");
                        return originalResponse.newBuilder()
                                .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                .build();
                    }
                })
                .build();
        try {
            Response response = client.newCall(request).execute();
            Log.e("mijie", "ok :  " + response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * post 数据请求
     */
    private void postData() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.SECONDS)
                .writeTimeout(10000, TimeUnit.SECONDS)
                .readTimeout(20000, TimeUnit.SECONDS)
                .build();//创建client

        //  OkHttpClient client = new OkHttpClient();//创建client

        //  client.Builder
        if (mPath == null) {
            //showTip("没有可以上传的文件");
            return;
        }
        // File file = new File("/storage/emulated/0/voicerecoder/CameraSample/VID_20170629_180119.mp4");
        File file = new File(mPath);
        if (!file.exists()) {
            showTip("文件不存在，请修改文件路径");
            return;
        }
        Log.e("mijie", " postData ");
        if (file.length() / 1024 / 1024f > 10) {
            showTip("文件太大无法上传");
        }
        //获取经纬度
        ArrayList<String> locations = LocationTool.getInstance().getLocationData(this);
        if (locations == null && locations.size() != 2) {
            showTip("获取经纬度异常");
            return;
        }
        Log.e("mijie", " 开始上传服务器 ");
        RequestBody fileBody = RequestBody.create(MediaType.parse("video/3gp"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "test.mp4", fileBody)
                .addFormDataPart("mac", "20:0c:c8:0d:20:d6")
                .addFormDataPart("longitude", "123.393303")
                .addFormDataPart("latitude", "35.121527")
                .addFormDataPart("description", "test")
                .build();
        Request request = new Request.Builder().url(Constant.BASE_URI).post(requestBody).build();//创建Request
        try {
            Response response = client.newCall(request).execute(); //执行上传

            ResultBean result = JSON.parseObject(response.body().string(), ResultBean.class);
            if (result.getErrorCode() == 0) {
                mHandler.sendEmptyMessage(DISPLAY_TYPE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

//        File file = new File("/storage/emulated/0/voicerecoder/CameraSample/VID_20170628_095258.mp4");
//        if (!file.exists())
//        {
//            Toast.makeText(MainActivity.this, "文件不存在，请修改文件路径", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        Map<String, String> params = new HashMap<>();
//        params.put("mac","adfasdfadff12bbb");
//        params.put("longitude","121.417074");
//        params.put("latitude","31.188535");
//        params.put("description","ceshi");
//        params.put("description","ceshi");
//        params.put("Content-Type","video/3gp");
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Content-Type", "video/3gp");
//        OkHttpUtils
//                .post()
//
//                //.addFile("mFile","VID_20170628_095258.mp4",file)
//                .url(Constant.BASE_URI)
//                .params(params)
//               // .headers(headers)
//               // .addParams("Content-Type","video/3gp")
//                .build()
//                .execute(new StringCallback() {
//                    @Override
//                    public void onError(Call call, Exception e, int id) {
//                        Log.e(TAG, "net error : " + e.toString());
//                    }
//
//                    @Override
//                    public void inProgress(float progress, long total, int id) {
//                        super.inProgress(progress, total, id);
//                        Log.e(TAG, "progress : " + progress + "   ;total : "+total);
//                    }
//
//                    @Override
//                    public void onResponse(String response, int id) {
//                        Log.e(TAG, "response :  " + response);
//                        // parseData(response);
//                    }
//                });
    }


//    //多文件上传（带进度）
//    private void upload() {
//        //这个是非ui线程回调，不可直接操作UI
//        final ProgressListener progressListener = new ProgressListener() {
//            @Override
//            public void onProgress(long bytesWrite, long contentLength, boolean done) {
//                Log.i("TAG", "bytesWrite:" + bytesWrite);
//                Log.i("TAG", "contentLength" + contentLength);
//                Log.i("TAG", (100 * bytesWrite) / contentLength + " % done ");
//                Log.i("TAG", "done:" + done);
//                Log.i("TAG", "================================");
//            }
//        };
//
//
//        //这个是ui线程回调，可直接操作UI
//        UIProgressListener uiProgressRequestListener = new UIProgressListener() {
//            @Override
//            public void onUIProgress(long bytesWrite, long contentLength, boolean done) {
//                Log.i("TAG", "bytesWrite:" + bytesWrite);
//                Log.i("TAG", "contentLength" + contentLength);
//                Log.i("TAG", (100 * bytesWrite) / contentLength + " % done ");
//                Log.i("TAG", "done:" + done);
//                Log.i("TAG", "================================");
//                //ui层回调,设置当前上传的进度值
//                int progress = (int) ((100 * bytesWrite) / contentLength);
////                uploadProgress.setProgress(progress);
////                uploadTV.setText("上传进度值：" + progress + "%");
//            }
//
//            //上传开始
//            @Override
//            public void onUIStart(long bytesWrite, long contentLength, boolean done) {
//                super.onUIStart(bytesWrite, contentLength, done);
//                Toast.makeText(getApplicationContext(),"开始上传",Toast.LENGTH_SHORT).show();
//            }
//
//            //上传结束
//            @Override
//            public void onUIFinish(long bytesWrite, long contentLength, boolean done) {
//                super.onUIFinish(bytesWrite, contentLength, done);
//                //uploadProgress.setVisibility(View.GONE); //设置进度条不可见
//                Toast.makeText(getApplicationContext(),"上传成功",Toast.LENGTH_SHORT).show();
//
//            }
//        };
//
//        HashMap<String, String> params = new HashMap<>();
//        params.put("mac","adfasdfadff12bbb");
//        params.put("longitude","121.417074");
//        params.put("latitude","31.188535");
//        params.put("description","ceshi");
//        params.put("description","ceshi");
//        params.put("Content-Type","video/3gp");
//        //开始Post请求,上传文件
//       OKHttpUtils.doPostRequest(Constant.BASE_URI, params, initUploadFile(), new Callback() {
//           @Override
//           public void onFailure(Call call, IOException e) {
//Log.e(TAG,"fail");
//           }
//
//           @Override
//           public void onResponse(Call call, Response response) throws IOException {
//               Log.e(TAG,"ok --> "+ response.body().toString());
//           }
//       });
//
//    }
//
//
//    //初始化上传文件的数据
//    private List<String> initUploadFile(){
//        List<String> fileNames = new ArrayList<>();
//        fileNames.add("/storage/emulated/0/voicerecoder/CameraSample/VID_20170628_095258.mp4"); //视频
//        return fileNames;
//    }


    /**
     * 　解析网络数据
     */
    private void parseData(String json) {

//        ResultBean result = JSON.parseObject(json, ResultBean.class);
//
//        showTip("联网请求成功，本次数据为==>>> " + result.getAlevel());


        ResultBean result = JSON.parseObject(json, ResultBean.class);
        if (result.getErrorCode() == 0) {
            showTip("视频上传成功");
        }
    }

    private void initCameraView() {
        iv_recoder = (ImageView) findViewById(R.id.iv_recoder);
        mPreview = (TextureView) findViewById(R.id.surface_view);
        mPreview.setSurfaceTextureListener(new CustomSurfaceViewListener());
    }

    /**
     * camera TextureView 监听类
     */
    class CustomSurfaceViewListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            mCamera = Camera.open();
            AppUtils.setCameraDisplayOrientation(MainActivity.this, 0, mCamera);
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException t) {
                Log.e(TAG, "onSurfaceTextureAvailable=== error:   " + t.toString());
            }
            mCamera.startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    }

    private void showTip(final String str) {
       /* Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        toast.setText(str);
        toast.show();*/

        Log.e("mijie", str);
    }

    //start vioce
    private void initVoiceView() {
        bt_speech = (Button) findViewById(R.id.bt_speech);
        et_voice_result = (EditText) findViewById(R.id.et_voice_result);
        RadioGroup group = (RadioGroup) findViewById(R.id.radioGroup);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (null == mIat) {
                    // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
                    showTip("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
                    return;
                }

                switch (checkedId) {
                    case R.id.iatRadioCloud:
                        mEngineType = SpeechConstant.TYPE_CLOUD;
                        break;
                    case R.id.iatRadioLocal:
                        mEngineType = SpeechConstant.TYPE_LOCAL;
                        /**
                         * 选择本地听写 判断是否安装语记,未安装则跳转到提示安装页面
                         */
                        if (!SpeechUtility.getUtility().checkServiceInstalled()) {
                            mInstaller.install();
                        } else {
                            String result = FucUtil.checkLocalResource();
                            if (!TextUtils.isEmpty(result)) {
                                showTip(result);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
                setSpeedState();
            }
        }
    };

    private void startCollectVioce() {
        isSpeeding = true;
        setSpeedState();
        FlowerCollector.onEvent(this, "iat_recognize");
        et_voice_result.setText(null);// 清空显示内容
        //mIatResults.clear();
        // 设置参数
        setParam();
        boolean isShowDialog = mSharedPreferences.getBoolean(
                getString(R.string.pref_key_iat_show), false);

        Log.e(TAG, "语言： " + mIat.getParameter(SpeechConstant.LANGUAGE) + " VAD_BOS : " + mIat.getParameter(SpeechConstant.VAD_BOS)
                + "  ;VAD_EOS:" + mIat.getParameter(SpeechConstant.VAD_EOS) + "  ;是否显示听写UI : " + isShowDialog);
        if (isShowDialog) {
            // 显示听写对话框
            mIatDialog.setListener(mRecognizerDialogListener);
            mIatDialog.show();
            showTip(getString(R.string.text_begin));
        } else {
            // 不显示听写对话框
            int ret = 0; // 函数调用返回值
            ret = mIat.startListening(mRecognizerListener);
            if (ret != ErrorCode.SUCCESS) {
                showTip("听写失败,错误码：" + ret);
                isSpeeding = false;
            } else {
                showTip(getString(R.string.text_begin));
            }
        }
    }

    /**
     * 停止收集语音
     */
    private void stopCollectVioce() {
        isSpeeding = false;
        setSpeedState();
        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

    /**
     * 听写监听器,无UI
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            showTip(error.getPlainDescription(true));
            isSpeeding = false;
            setSpeedState();
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            setSpeedState();
            isSpeeding = false;
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            printResult(results);

            if (isLast) {
                // TODO 最后的结果
                isSpeeding = false;
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            showTip("当前正在说话，音量大小：" + volume);
//            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {// isLast 监听到没有说话时，停止语音听写
            if (isLast) {
                isSpeeding = false;
                setSpeedState();
            }
            printResult(results);
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
            setSpeedState();
        }

    };

    /**
     * 解析　RecognizerResult
     */
    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        Log.e(TAG, "printResult : " + text);

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        if (getString(R.string.start_recoder).equals(text.trim())) {
            startRecord();
        } else if (getString(R.string.stop_recoder).equals(text.trim())) {
            stopRecord();
        }

        et_voice_result.setText(resultBuffer.toString());
        et_voice_result.setSelection(et_voice_result.length());
    }


    /**
     * 采集器参数设置
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }
        Log.e(TAG, "iat_vadbos_preference :  " + mSharedPreferences.getString("iat_vadbos_preference", "60000"));
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "60000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "60000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "0"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }
    //end vioce


    /**
     * 6.0以上，动态申请权限
     */
    public void checkoutPermissions() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if ((Build.VERSION.SDK_INT < 23) || hasPermissions(perms)) {//已经具有相关的权限
            gotoApp();
        } else {
            gainPermission(getString(R.string.permissions_fail), new PermissionCallback() {
                @Override
                public void gainPermissionSuccess() {
                    gotoApp();
                }

                @Override
                public void gainPermissionFail() {
                    showTip(getString(R.string.permissions_fail));
                }
            }, perms);
        }

//        if (Build.VERSION.SDK_INT > 22) {
//            if (isPermissionGranted(Manifest.permission.CAMERA)
//                    || isPermissionGranted(Manifest.permission.RECORD_AUDIO)
//                    || isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                //申请camera权限
//                this.requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_OK);
//            } else {
//                //已经获取到摄像头权限
//                //  prepareVideoRecorder();
//                setSpeedState(false);
//            }
//        } else {
//            //6.0以下不需要动态获取权限
//            setSpeedState(true);
//            //  prepareVideoRecorder();
//        }
    }


//    /**
//     * 判断权限是否已经获取
//     */
//    private boolean isPermissionGranted(String permission) {
//        return ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED;
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch (requestCode) {
//            case CAMERA_OK:
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    //已获取权限,初始化预览界面
//                } else {
//                    //这里是拒绝给APP摄像头权限，给个提示什么的说明一下都可以。
//                    Toast.makeText(MainActivity.this, "请手动打开相机权限", Toast.LENGTH_SHORT).show();
//                    setSpeedState(false);
//                }
//                break;
//            default:
//                break;
//        }
//
//    }

    private void stopRecord() {
        Log.e(TAG, "stopRecord : " + isRecording);
        if (isRecording) {
            try {
                mMediaRecorder.stop();  // stop the recording
                iv_recoder.setVisibility(View.INVISIBLE);
                postDataToServer();

            } catch (RuntimeException e) {
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                mOutputFile.delete();
            }
            releaseMediaRecorder();
            mCamera.lock();

            isRecording = false;
            releaseCamera();

        }
    }

    private void postDataToServer() {
        new Thread() {
            @Override
            public void run() {
                postData();
            }
        }.start();
    }


    /**
     * 开始录像
     */
    public void startRecord() {
        if (!isRecording) {
            new MediaPrepareTask().execute(null, null, null);
        }
    }


    /**
     * 开始录像true，停止录像　false
     */
    private void setSpeedState() {
        if (isSpeeding) {
            isSpeeding = true;
            iv_settings.setClickable(false);
            bt_speech.setText(getString(R.string.stop_speed));
            bt_speech.setTextColor(Color.RED);
        } else {
            isSpeeding = false;
            iv_settings.setClickable(true);
            bt_speech.setText(getString(R.string.start_speed));
            bt_speech.setTextColor(Color.BLACK);
        }
    }

    /**
     * 释放MediaRecorder资源
     */
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaRecorder();
        releaseCamera();
        stopCollectVioce();
    }

    /**
     * 释放ｃａｍｅｒａ资源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }


    private boolean prepareVideoRecorder() {
        mCamera = CameraHelper.getDefaultCameraInstance();

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording mProfile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = PREVIEW_WIGTH;//optimalSize.width;
        profile.videoFrameHeight =PREVIEW_HIGTH; //optimalSize.height;
        Log.e("mijie", "videoFrameWidth : " + profile.videoFrameWidth + "    ;videoFrameHeight : " + profile.videoFrameHeight);
        parameters.setPreviewSize(PREVIEW_WIGTH, PREVIEW_HIGTH);
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }

        mMediaRecorder = new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOrientationHint(180);
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setVideoFrameRate(15);
        mMediaRecorder.setVideoEncodingBitRate(1 * 1024 * 1024);

        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        mPath = mOutputFile.getAbsolutePath();
        Log.e("mijie", "mOutputFile name :  " + mOutputFile.getAbsolutePath());

        if (mOutputFile == null) {
            return false;
        }
        mMediaRecorder.setOutputFile(mOutputFile.getPath());

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_speech:
                collectVioceing();
                break;
            case R.id.iv_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            case R.id.bt_face_detect:
                releaseMediaRecorder();
                releaseCamera();
                stopCollectVioce();
                startActivity(new Intent(this, FaceDetectActivity.class));
                finish();
                break;
        }
    }

    /**
     * 声音听写
     */
    private void collectVioceing() {
        if (isSpeeding) {
            stopCollectVioce();
        } else {
            startCollectVioce();
        }
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                mMediaRecorder.start();
                isRecording = true;
                mHandler.sendEmptyMessage(DISPLAY_VIEW);
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                MainActivity.this.finish();
            }
        }
    }
}
