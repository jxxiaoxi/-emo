package com.mj.voicerecoder;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.FaceRequest;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.RequestListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.sunflower.FlowerCollector;
import com.mj.commonlib.http.helper.OkHttpHelper;
import com.mj.commonlib.http.helper.ResultCallBack;
import com.mj.voicerecoder.base.BaseActivity;
import com.mj.voicerecoder.bean.JpushBean;
import com.mj.voicerecoder.bean.ResultBean;
import com.mj.voicerecoder.constant.Constant;
import com.mj.voicerecoder.db.greendao.DaoSession;
import com.mj.voicerecoder.face.util.FaceUtil;
import com.mj.voicerecoder.media.CameraHelper;
import com.mj.voicerecoder.speech.setting.Settings;
import com.mj.voicerecoder.speech.util.ApkInstaller;
import com.mj.voicerecoder.speech.util.FucUtil;
import com.mj.voicerecoder.speech.util.JsonParser;
import com.mj.voicerecoder.util.AppUtils;
import com.mj.voicerecoder.util.LocationTool;
import com.mj.voicerecoder.util.Voice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final int DISPLAY_VIEW = 10086;
    private static final int BIMAP_OK = 300000;
    private static final int CAMERA_ID = 1;
    private static final int STOP_RECODER_EVENT = 999;
    //    private static final long RECODER_TIME = 30000;
    private static final int SHOW_TIP = 5000;
    private Camera mCamera;
    private TextureView mPreview;
    private Button iv_settings;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;
    private ImageView iv_recoder;

    private boolean isRecording = false;
    private static final String TAG = "mijie";
    private Button bt_speech;
    private boolean isSpeeding = false;
    private static final int DISPLAY_TYPE = 2000;
    private static final int PREVIEW_HIGTH = 480;
    private static final int PREVIEW_WIGTH = 640;
    //是否开启人脸识别
    private boolean isDetect = false;
    private FaceTask mFaceTask;
    // authid为6-18个字符长度，用于唯一标识用户
    private String mAuthid = null;
    // 进度对话框
    private ProgressDialog mProDialog;
    // FaceRequest对象，集成了人脸识别的各种功能
    private FaceRequest mFaceRequest;
    private final int REQUEST_PICTURE_CHOOSE = 1;
    private final int REQUEST_CAMERA_IMAGE = 2;
    // 拍照得到的照片文件
    private File mPictureFile;
    private Bitmap mImage = null;
    private byte[] mImageData = null;
    public static final int REPREVIEW = 9999;
    public static boolean TAKE_PIC = false;
    private long mTime;
    //推送开始的标记,在自动发现人脸中用到
    private boolean isJpushStart = false;

    @BindView(R.id.surface_view)
    TextureView surfaceView;
    @BindView(R.id.bt_register)
    Button btRegister;
    @BindView(R.id.online_verify)
    Button btVerify;
    @BindView(R.id.online_authid)
    EditText onlineAuthid;
    @BindView(R.id.online_pick)
    Button onlinePick;
    @BindView(R.id.online_camera)
    Button onlineCamera;
    @BindView(R.id.bt_detect)
    Button btDetect;

    //start vioce
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;

    private TextView et_voice_result;
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

    //记录经纬度
    public ArrayList<String> mLocations;

    private DaoSession mUserDao;
    private String mPath;
    public static boolean isForeground = false;
    private static final int YUV_1080_SIZE = 3110400;
    private byte[] mImageCallbackBuffer = new byte[YUV_1080_SIZE];
    public static final int TAKE_PIC_TIME = 10000;
    private boolean isStartByspeeh = false;

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISPLAY_VIEW:
                    showIVSpeedView();
                    break;
                case DISPLAY_TYPE:
                    showToast();
                    break;
                case BIMAP_OK:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    saveJpushicon(bitmap);
                    break;
                case STOP_RECODER_EVENT:
                    Log.e(TAG, "STOP_RECODER_EVENT");
                    stopRecord();
                    break;
                case REPREVIEW:
                    rePlayPreview();
                    break;
                case SHOW_TIP:
                    showFaceTip((String) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 保存jpush图片
     */
    private void saveJpushicon(Bitmap bitmap) { //registerFace()
        String url = AppUtils.saveBitmapToFile(this, bitmap);
        if (url != null) {//当jpsuh推送的图片保存后，注册到讯飞服务器，进行人脸识别
            isJpushStart = true;
            onlineAuthid.setText(AppUtils.getAuthid());//自动设置ID
            Log.e(TAG, "imagetoByte");
            imagetoByte(url);
            Log.e(TAG, "registerFace");
            registerFace();//自动进行人脸注册
        }
    }


    public void showToast() {
        Toast.makeText(this, "视频上传成功", Toast.LENGTH_SHORT).show();
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
        iv_settings = (Button) findViewById(R.id.iv_settings);
        iv_settings.setOnClickListener(this);
        checkoutPermissions();//获取权限

        registerMessageReceiver();
    }


    /**
     * 初始化app
     */
    private void initApp() {
        initCameraView();//初始化camera view
        initVoiceView();//初始voice view
        //获取位置
        mLocations = LocationTool.getInstance().getLocationData(this);

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

        //face start
        mProDialog = new ProgressDialog(MainActivity.this);
        mProDialog.setCancelable(true);
        mProDialog.setTitle("请稍后");

        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                // cancel进度框时,取消正在进行的操作
                if (null != mFaceRequest) {
                    mFaceRequest.cancel();
                }
            }
        });
        mFaceRequest = new FaceRequest(this);
        ButterKnife.bind(this);
        //face end
    }

    /**
     * post 数据请求
     */
    private void postDataToServer() {
        if (mPath == null) {
            asynShowTip("没有可以上传的文件");
            return;
        }
        //File file = new File("/storage/emulated/0/voicerecoder/CameraSample/VID_20170704_113536.mp4");
        File file = new File(mPath);
        if (!file.exists()) {
            asynShowTip("文件不存在，请修改文件路径");
            return;
        }
        Log.e(TAG, " postData ");
        if (file.length() / 1024 / 1024f > 10) {
            asynShowTip("文件太大无法上传");
        }

        if (mLocations == null && mLocations.size() != 2) {
            asynShowTip("获取经纬度异常");
            return;
        }


        Log.e(TAG, " 开始上传服务器 " + "mac : " + AppUtils.getWifiMacAddress(MainActivity.this) + " ;longitude: " + mLocations.get(0) + " ;latitude: " + mLocations.get(1));
        RequestBody fileBody = RequestBody.create(MediaType.parse("video/3gp"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "test.mp4", fileBody)
                .addFormDataPart("mac", AppUtils.getWifiMacAddress(MainActivity.this))
                .addFormDataPart("longitude", mLocations.get(0))
                .addFormDataPart("latitude", mLocations.get(1))
                .addFormDataPart("description", "监控视频上传")
                .build();
        OkHttpHelper.getOkHttpInstance(this).uploadFiles(Constant.BASE_URI, requestBody, new ResultCallBack() {
            @Override
            public void onSuccess(Response response) {
                ResultBean result = null;
                try {
                    result = JSON.parseObject(response.body().string(), ResultBean.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (result.getErrorCode() == 0) {
                    //mHandler.sendEmptyMessage(DISPLAY_TYPE);
                    asynShowTip("上传成功");
                }
            }

            @Override
            public void onFailure(IOException failstr) {
                asynShowTip("上传失败");
            }
        });
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
            safeCameraOpen(CAMERA_ID);
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
                Log.e(TAG, "onSurfaceTextureDestroyed  ");
                mCamera.stopPreview();
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    }


    /**
     * 打开camera
     */
    private boolean safeCameraOpen(int id) {
        boolean opened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                    mSupportedPreviewSizes, surfaceView.getWidth(), surfaceView.getHeight());

            // Use the same size for recording mProfile.
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            profile.videoFrameWidth = optimalSize.width;
            profile.videoFrameHeight = optimalSize.height;

            parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
            mCamera.setParameters(parameters);
            AppUtils.setCameraDisplayOrientation(this, id, mCamera);
            setPreviewFrame();
            opened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "fail to open camera");
            e.printStackTrace();
        }
        return opened;
    }

    /**
     * 每次录像后，必须重新设置，不然无法监听到PreviewCallback
     */
    public void setPreviewFrame() {
        mCamera.addCallbackBuffer(mImageCallbackBuffer);

        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                // Log.e(TAG, " onPreviewFrame : " + isDetect);
                if ((System.currentTimeMillis() - mTime) > TAKE_PIC_TIME && isDetect) {
                    mTime = System.currentTimeMillis();
                    TAKE_PIC = false;
                    if (null != mFaceTask) {
                        switch (mFaceTask.getStatus()) {
                            case RUNNING:
                                return;
                            case PENDING:
                                mFaceTask.cancel(false);
                                break;
                        }
                    }
                    mFaceTask = new FaceTask(bytes);
                    mFaceTask.execute((Void) null);
                }
                if (mCamera != null) {
                    mCamera.addCallbackBuffer(bytes);
                }
            }
        });
    }

    private void releaseCameraAndPreview() {
        Log.e(TAG, "releaseCameraAndPreview  ");
        //mPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }


    private void showTip(final String str) {
       /* Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        toast.setText(str);
        toast.show();*/

        Log.e(TAG, str);
    }

    //start vioce
    private void initVoiceView() {
        bt_speech = (Button) findViewById(R.id.bt_speech);
        et_voice_result = (TextView) findViewById(R.id.et_voice_result);
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
                setSpeedState();
//                stopRecord();
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

        Log.e(TAG, "jpush context : " + AppUtils.getJpushText(MainActivity.this));
        if (text.trim() != null && text.contains((AppUtils.getJpushText(MainActivity.this)))) {
            isStartByspeeh = true;
            startRecord();
        }

//        if (getString(R.string.start_recoder).equals(text.trim())) {
//            startRecord();
//        } else if (getString(R.string.stop_recoder).equals(text.trim())) {
//            stopRecord();
//        }

        et_voice_result.setText(resultBuffer.toString());
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
            initApp();
        } else {
            gainPermission(getString(R.string.permissions_fail), new PermissionCallback() {
                @Override
                public void gainPermissionSuccess() {
                    initApp();
                    rePlayPreview();
                }

                @Override
                public void gainPermissionFail() {
                    showTip(getString(R.string.permissions_fail));
                }
            }, perms);
        }
    }

    /**
     * 停止录像
     */
    private synchronized void stopRecord() {
        Log.e(TAG, "stopRecord : " + isRecording);
        if (isRecording) {
            try {
                mMediaRecorder.stop();  // stop the recording
                iv_recoder.setVisibility(View.INVISIBLE);
                if (isStartByspeeh) {//如果暂停前的操作是语音识别，那么停止录音后则开启语音识别
                    collectVioceing();
                    isStartByspeeh = false;
                }
                postDataToServer();

            } catch (RuntimeException e) {
                Log.e(TAG,"RuntimeException ==> "+e.getMessage());
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                mOutputFile.delete();
            }
            releaseMediaRecorder();
            if (mCamera != null)
                mCamera.lock();

            isRecording = false;
            //releaseCamera();
            rePlayPreview();

        }
    }


    /**
     * 开始录像
     */
    public synchronized void startRecord() {
        Log.e(TAG, "startRecord : " + isRecording);
        if (!isRecording) {
            new MediaPrepareTask().execute(null, null, null);

            collectVioceing();//开始录像，如果语音识别还在，则关闭

            //开启录制后，20s停止录制
            Message message = new Message();
            message.what = STOP_RECODER_EVENT;
            mHandler.sendMessageDelayed(message, Constant.RECODER_TIME);//如果没有其他的手动暂停,30s后自动暂停
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
        if (mFaceTask != null && mFaceTask.getStatus() != AsyncTask.Status.FINISHED) {
            mFaceTask.cancel(true);
        }

        if (mMessageReceiver != null)
            unregisterReceiver(mMessageReceiver);
    }

    /**
     * 释放ｃａｍｅｒａ资源
     */
    private void releaseCamera() {
        Log.e(TAG, "releaseCamera ");
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }


    /**
     * 初始化　MediaRecorder
     */
    private boolean prepareVideoRecorder() {
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = PREVIEW_WIGTH;//optimalSize.width;
        profile.videoFrameHeight = PREVIEW_HIGTH; //optimalSize.height;

        mMediaRecorder = new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        Log.e(TAG, " mCamera : " + mCamera);
        if (mCamera != null) {
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
        }

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
        Log.e(TAG, "mOutputFile name :  " + mOutputFile.getAbsolutePath());

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
                isStartByspeeh = false;
                collectVioceing();
                break;
            case R.id.iv_settings:
                startActivity(new Intent(this, Settings.class));
                break;
        }
    }

    /**
     * 声音听写
     */
    private void collectVioceing() {
        if (isDetect) {
            //showFaceTip("人脸识别和语音听写暂不支持同时打开");
            return;
        }
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
                isRecording = false;
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


    @Override
    protected void onResume() {
        isForeground = true;
        super.onResume();
    }


    @Override
    protected void onPause() {
        isForeground = false;
        super.onPause();
    }


    // 处理Jpush消息
    //for receive customer msg from jpush server
    private MessageReceiver mMessageReceiver;
    public static final String MESSAGE_RECEIVED_ACTION = "com.example.jpushdemo.MESSAGE_RECEIVED_ACTION";
    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_EXTRAS = "extras";

    public void registerMessageReceiver() {
        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MESSAGE_RECEIVED_ACTION);
        registerReceiver(mMessageReceiver, filter);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.e(TAG, "MessageReceiver: " + intent.getAction());
                if (MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                    String messge = intent.getStringExtra(KEY_MESSAGE);
                    setCostomMsg(messge);
                }
            } catch (Exception e) {
            }
        }
    }

    private void setCostomMsg(String msg) {
        final ArrayList<JpushBean> results = (ArrayList<JpushBean>) JSON.parseArray(msg, JpushBean.class);
        if (results == null) {
            return;
        }
        Log.e(TAG, "setCostomMsg " + results.get(0).getContent());

        for (JpushBean jb : results) {
            if (jb.getType() == 2) {//发送的是文字
                if (jb.getContent() != null) {
                    for (String str : jb.getContent()) {
                        showFaceTip(str);
                        AppUtils.putJpushText(MainActivity.this, str);
                    }
                    //当推送过来的是指令时，自动开启语音收集
                    collectVioceing();
                }

            } else {
                if (jb.getContent() != null) {
                    for (final String str : jb.getContent()) {
                        showFaceTip("推送的是图片，开始下载");
                        downIcon(str);
                    }
                }
            }
        }
    }


    /**
     * 下载服务器图片 test
     */
    private void downIcon(String url) {
        Log.e(TAG, "URL " + url);
        OkHttpHelper.getOkHttpInstance(this).downLoadFiles(url, new ResultCallBack() {
            @Override
            public void onSuccess(Response response) {
                //获取流
                InputStream in = response.body().byteStream();
                //转化为bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                //使用Hanlder发送消息
                Message msg = Message.obtain();
                msg.what = BIMAP_OK;
                Log.e(TAG, "send");
                msg.obj = bitmap;
                mHandler.sendMessage(msg);
                updateGallery(AppUtils.getAppPath() + "jpush/");
            }

            @Override
            public void onFailure(IOException ex) {
                Log.e(TAG, "onFailure");
            }
        });
    }


    //face start

    /**
     * 重新PlayPreview
     */
    private void rePlayPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
            setPreviewFrame();
        }
    }

    @OnClick({R.id.online_pick, R.id.online_camera, R.id.bt_register, R.id.online_verify, R.id.bt_detect})
    public void onViewClicked(View view) {
        int ret = ErrorCode.SUCCESS;
        switch (view.getId()) {
            case R.id.bt_detect:
                Log.e(TAG, " bt_detect click : " + isDetect);
                detectFace();
                break;
            case R.id.online_pick:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, REQUEST_PICTURE_CHOOSE);
                break;
            case R.id.online_camera:
                takePhoto();
                break;
            case R.id.bt_register:
                registerFace();
                break;
            case R.id.online_verify:
                verifyFace();
                break;

        }
        if (ErrorCode.SUCCESS != ret) {
            mProDialog.dismiss();
            showFaceTip("出现错误：" + ret);
        }
    }

    /**
     * 人脸发现
     */
    private void detectFace() {
        Log.e(TAG, "detectFace 人脸发现");
        mAuthid = ((EditText) findViewById(R.id.online_authid)).getText().toString();
        Log.e(TAG, "detectFace AUTH_ID : " + mAuthid);
        if (TextUtils.isEmpty(mAuthid)) {
            asynShowTip("authid不能为空");
            return;
        }
        if (isDetect) {
            stopDetect();
        } else {
            startDetect();
        }
    }


    /**
     * 人脸验证
     */
    private void verifyFace() {
        mAuthid = ((EditText) findViewById(R.id.online_authid)).getText().toString();
        if (TextUtils.isEmpty(mAuthid)) {
            showFaceTip("authid不能为空");
            return;
        }

        if (null != mImageData) {
            mProDialog.setMessage("验证中...");
            mProDialog.show();
            // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
            // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。
            Log.e(TAG, "verifyFace AUTH_ID : " + mAuthid);
            mFaceRequest.setParameter(SpeechConstant.AUTH_ID, mAuthid);
            mFaceRequest.setParameter(SpeechConstant.WFR_SST, "verify");
            mFaceRequest.sendRequest(mImageData, mRequestListener);
        } else {
            showFaceTip("请选择图片后再验证");
        }
    }


    /**
     * 人脸注册
     */
    private void registerFace() {
        mAuthid = ((EditText) findViewById(R.id.online_authid)).getText().toString();
        if (TextUtils.isEmpty(mAuthid)) {
            showFaceTip("authid不能为空");
            return;
        }

        if (null != mImageData) {
            mProDialog.setMessage("注册中...");
            if (!isFinishing()) {//Dialog　依赖于当前activity，当activity消失后，显示dialog会报错
                mProDialog.show();
            }
            // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
            // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。
            Log.e(TAG, "registerFace AUTH_ID : " + mAuthid);
            mFaceRequest.setParameter(SpeechConstant.AUTH_ID, mAuthid);
            mFaceRequest.setParameter(SpeechConstant.WFR_SST, "reg");
            mFaceRequest.sendRequest(mImageData, mRequestListener);
        } else {
            showFaceTip("请选择图片后再注册");
        }
    }


    /**
     * 停止人脸识别
     */
    private void stopDetect() {
        isDetect = false;
//        if (isRecording) {
//            stopRecord();
//        }
        btDetect.setText(getString(R.string.start_detect));
    }

    /**
     * 开始人脸识别
     */
    private void startDetect() {
//        if (isSpeeding) {
//            showFaceTip("人脸识别和语音听写暂不支持同时打开");
//            return;
//        }
        isDetect = true;
        btDetect.setText(getString(R.string.stop_detect));
    }

    private RequestListener mRequestListener = new RequestListener() {

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            if (null != mProDialog) {
                mProDialog.dismiss();
            }

            try {
                String result = new String(buffer, "utf-8");
                JSONObject object = new JSONObject(result);
                String type = object.optString("sst");
                if ("reg".equals(type)) {
                    register(object);
                } else if ("verify".equals(type)) {
                    verify(object);
                }
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO: handle exception
            }
        }


        private void register(JSONObject obj) throws JSONException {
            int ret = obj.getInt("ret");
            if (ret != 0) {
                showFaceTip("注册失败");
                Toast.makeText(MainActivity.this, "注册失败", Toast.LENGTH_SHORT).show();
                return;
            }
            if ("success".equals(obj.get("rst"))) {
                showFaceTip("注册成功");
                if (isJpushStart) {
                    detectFace();
                    isJpushStart = false;
                }
                //
            } else {
                showFaceTip("注册失败");
            }
        }


        /**
         * 人脸验证结果
         */
        private void verify(JSONObject obj) throws JSONException {
            int ret = obj.getInt("ret");
            if (ret != 0) {
                showFaceTip("验证失败");
                return;
            }
            if ("success".equals(obj.get("rst"))) {
                if (obj.getBoolean("verf")) {
                    showFaceTip("通过验证，欢迎回来！");
                    if (Constant.IS_VOICE) {
                        Voice.getInstance(MainActivity.this).startSpeed("大家注意目标" + mAuthid + "号出现");
                    }
                    //目标出现，开始录像
                    startRecord();
                } else {
                    showFaceTip("验证失败，不是同一个人");
                }
            } else {
                showFaceTip("验证失败");
            }
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (null != mProDialog) {
                mProDialog.dismiss();
            }

            if (error != null) {
                switch (error.getErrorCode()) {
                    case ErrorCode.MSP_ERROR_ALREADY_EXIST:
                        showFaceTip("authid已经被注册，请更换后再试");
                        break;

                    default:
                        showFaceTip(error.getPlainDescription(true));
                        break;
                }
            }
        }
    };

    private void showFaceTip(String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }


    /**
     * 子线程中显示提示
     */
    private void asynShowTip(String str) {
        Message msg = new Message();
        msg.what = SHOW_TIP;
        msg.obj = str;
        mHandler.sendMessage(msg);
    }

    private class FaceTask extends AsyncTask<Void, Void, Void> {
        private byte[] mData;

        //构造函数
        FaceTask(byte[] data) {
            this.mData = data;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Camera.Size size = mCamera.getParameters().getPreviewSize(); //获取预览大小
            final int w = size.width;  //宽度
            final int h = size.height;
            final YuvImage image = new YuvImage(mData, ImageFormat.NV21, w, h, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
            if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
                return null;
            }
            byte[] tmp = os.toByteArray();
            Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
            //自己定义的实时分析预览帧视频的算法
            saveAndVerifyPic(bmp);
            return null;
        }

        private void saveAndVerifyPic(Bitmap bmp) {
            String fileSrc = null;
            if (null != bmp) {
                FaceUtil.saveBitmapToFile(MainActivity.this, bmp);
            }

            // 获取图片保存路径
            fileSrc = FaceUtil.getImagePath(MainActivity.this);
            imagetoByte(fileSrc);
            onlineVerify();
        }
    }

    /**
     * image 转化成Byte[]
     */
    public void imagetoByte(String url) {
        // 获取图片的宽和高
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        mImage = BitmapFactory.decodeFile(url, options);

        // 压缩图片
        options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
                (double) options.outWidth / 1024f,
                (double) options.outHeight / 1024f)));
        options.inJustDecodeBounds = false;
        mImage = BitmapFactory.decodeFile(url, options);


        // 若mImageBitmap为空则图片信息不能正常获取
        if (null == mImage) {
            showFaceTip("图片信息无法正常获取！");
            return;
        }

        // 部分手机会对图片做旋转，这里检测旋转角度
        int degree = FaceUtil.readPictureDegree(url);
        if (degree != 0) {
            // 把图片旋转为正的方向
            mImage = FaceUtil.rotateImage(degree, mImage);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        //可根据流量及网络状况对图片进行压缩
        mImage.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        mImageData = baos.toByteArray();
    }


    private void takePhoto() {
        if (mCamera == null)
            return;

        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Matrix matrix = new Matrix();
                //设置缩放
                matrix.postScale(1.0f, 1.0f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                String times = format.format((new Date()));
                String path = AppUtils.getAppPath() + times + ".jpg";

                File file = new File(path);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try {
                    FileOutputStream outStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.close();
                    FaceUtil.readPictureDegree(path);
                    updateGallery(file.getAbsolutePath());
                    mHandler.sendEmptyMessage(REPREVIEW);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }


    /**
     * 在线人脸验证
     */
    private void onlineVerify() {
        if (null != mImageData) {
            //mProDialog.setMessage("验证中...");
            Log.e(TAG, "验证中...");
            //mProDialog.show();
            // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
            // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。
            mFaceRequest.setParameter(SpeechConstant.AUTH_ID, mAuthid);
            mFaceRequest.setParameter(SpeechConstant.WFR_SST, "verify");
            mFaceRequest.sendRequest(mImageData, mRequestListener);
        } else {
            asynShowTip("请选择图片后再验证");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        String fileSrc = null;
        if (requestCode == REQUEST_PICTURE_CHOOSE) {
            if ("file".equals(data.getData().getScheme())) {
                // 有些低版本机型返回的Uri模式为file
                fileSrc = data.getData().getPath();
            } else {
                // Uri模型为content
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(data.getData(), proj,
                        null, null, null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                fileSrc = cursor.getString(idx);
                cursor.close();
            }
            // 跳转到图片裁剪页面
            FaceUtil.cropPicture(this, Uri.fromFile(new File(fileSrc)));
        } else if (requestCode == REQUEST_CAMERA_IMAGE) {
            if (null == mPictureFile) {
                showFaceTip("拍照失败，请重试");
                return;
            }

            fileSrc = mPictureFile.getAbsolutePath();
            updateGallery(fileSrc);
            // 跳转到图片裁剪页面
            FaceUtil.cropPicture(this, Uri.fromFile(new File(fileSrc)));
        } else if (requestCode == FaceUtil.REQUEST_CROP_IMAGE) {
            // 获取返回数据
            Bitmap bmp = data.getParcelableExtra("data");
            // 若返回数据不为null，保存至本地，防止裁剪时未能正常保存
            if (null != bmp) {
                FaceUtil.saveBitmapToFile(this, bmp);
            }
            // 获取图片保存路径
            fileSrc = FaceUtil.getImagePath(this);

            imagetoByte(fileSrc);
        }

    }

    /**
     * 更新图库可以查看到的所以图片
     */
    private void updateGallery(String filename) {
        MediaScannerConnection.scanFile(this, new String[]{filename}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

    @Override
    public void finish() {
        if (null != mProDialog) {
            mProDialog.dismiss();
        }
        super.finish();
    }
    //face end

}
