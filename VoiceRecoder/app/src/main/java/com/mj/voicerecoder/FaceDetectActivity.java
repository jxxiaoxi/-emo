package com.mj.voicerecoder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.FaceRequest;
import com.iflytek.cloud.RequestListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.mj.voicerecoder.base.BaseActivity;
import com.mj.voicerecoder.face.util.FaceUtil;
import com.mj.voicerecoder.media.CameraHelper;
import com.mj.voicerecoder.util.AppUtils;
import com.mj.voicerecoder.util.Voice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by liuwei on 6/24/17.
 * 用于人脸识别
 */

public class FaceDetectActivity extends BaseActivity {
    private  int CAMERA_ID = 1;
    @BindView(R.id.surface_view)
    TextureView surfaceView;
    @BindView(R.id.bt_register)
    Button btRegister;
    @BindView(R.id.online_verify)
    Button btDetect;
    @BindView(R.id.online_authid)
    EditText onlineAuthid;
    @BindView(R.id.online_pick)
    Button onlinePick;
    @BindView(R.id.online_camera)
    Button onlineCamera;
    //    @BindView(R.id.online_img)
//    ImageView onlineImg;
    @BindView(R.id.iv_swich_camera)
    ImageView ivSwichCamera;


    private Camera mCamera;
    private static final int YUV_1080_SIZE = 3110400;
    private byte[] mImageCallbackBuffer = new byte[YUV_1080_SIZE];
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
    public static final int START_PREVIEW = 10000;
    public static final int TAKE_PIC_TIME = 10000;
    public static boolean TAKE_PIC = false;
    private long mTime;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REPREVIEW:
                    reDisPlayPreview();
                    break;
                case TAKE_PIC_TIME:
                    TAKE_PIC = true;
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        ButterKnife.bind(this);
        surfaceView.setSurfaceTextureListener(new CoustomFaceViewListener());

        mProDialog = new ProgressDialog(this);
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
    }

    @Override
    public boolean showActionBar() {
        return false;
    }

    private RequestListener mRequestListener = new RequestListener() {

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.e("liuwei", "onBufferReceived  " + mProDialog);
            if (null != mProDialog) {
                mProDialog.dismiss();
            }

            try {
                String result = new String(buffer, "utf-8");
                Log.e("liuwei", "000  : " + result);

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
                showTip("注册失败");
                return;
            }
            if ("success".equals(obj.get("rst"))) {
                showTip("注册成功");
            } else {
                showTip("注册失败");
            }
        }


        private void verify(JSONObject obj) throws JSONException {
            int ret = obj.getInt("ret");
            if (ret != 0) {
                showTip("验证失败");
                return;
            }
            if ("success".equals(obj.get("rst"))) {
                if (obj.getBoolean("verf")) {
                    showTip("通过验证，欢迎回来！");
                    Voice.getInstance(FaceDetectActivity.this).startSpeed(mAuthid+"出现");
                } else {
                    showTip("验证失败，不是同一个人");
                }
            } else {
                showTip("验证失败");
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
                        showTip("authid已经被注册，请更换后再试");
                        break;

                    default:
                        showTip(error.getPlainDescription(true));
                        break;
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @OnClick({R.id.online_pick, R.id.online_camera, R.id.bt_register, R.id.online_verify, R.id.iv_swich_camera})
    public void onViewClicked(View view) {
        int ret = ErrorCode.SUCCESS;
        switch (view.getId()) {
//            case R.id.iv_swich_camera:
//                swichCamera();
//                break;
            case R.id.online_pick:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, REQUEST_PICTURE_CHOOSE);
                break;
            case R.id.online_camera:
//                // 设置相机拍照后照片保存路径
//                mPictureFile = new File(Environment.getExternalStorageDirectory(),
//                        "picture" + System.currentTimeMillis() / 1000 + ".jpg");
//                // 启动拍照,并保存到临时文件
//                Intent mIntent = new Intent();
//                mIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
//                mIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPictureFile));
//                mIntent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
//                startActivityForResult(mIntent, REQUEST_CAMERA_IMAGE);
                takePhoto();

                break;
            case R.id.bt_register:
                mAuthid = ((EditText) findViewById(R.id.online_authid)).getText().toString();
                if (TextUtils.isEmpty(mAuthid)) {
                    showTip("authid不能为空");
                    return;
                }

                if (null != mImageData) {
                    mProDialog.setMessage("注册中...");
                    mProDialog.show();
                    // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
                    // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。
                    mFaceRequest.setParameter(SpeechConstant.AUTH_ID, mAuthid);
                    mFaceRequest.setParameter(SpeechConstant.WFR_SST, "reg");
                    ret = mFaceRequest.sendRequest(mImageData, mRequestListener);
                } else {
                    showTip("请选择图片后再注册");
                }
                break;
            case R.id.online_verify:
                mAuthid = ((EditText) findViewById(R.id.online_authid)).getText().toString();
                if (TextUtils.isEmpty(mAuthid)) {
                    showTip("authid不能为空");
                    return;
                }

                if (null != mImageData) {
                    mProDialog.setMessage("验证中...");
                    mProDialog.show();
                    // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
                    // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。
                    mFaceRequest.setParameter(SpeechConstant.AUTH_ID, mAuthid);
                    mFaceRequest.setParameter(SpeechConstant.WFR_SST, "verify");
                    ret = mFaceRequest.sendRequest(mImageData, mRequestListener);
                } else {
                    showTip("请选择图片后再验证");
                }
                break;

        }
        if (ErrorCode.SUCCESS != ret) {
            mProDialog.dismiss();
            showTip("出现错误：" + ret);
        }
    }

    private void swichCamera() {
        stopPreviewAndFreeCamera();
        if(CAMERA_ID ==0){
            CAMERA_ID = 1;
        }else{
            CAMERA_ID = 0;
        }
        safeCameraOpen(CAMERA_ID);
        reDisPlayPreview();
    }

    class CoustomFaceViewListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e("mijie", "onSurfaceTextureAvailable ");
            //现在大小已经知道了，设置相机参数开启Preview
            if (safeCameraOpen(CAMERA_ID)) {
                disPalyPreview(surfaceTexture);
            }


        }


        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e("mijie", "onSurfaceTextureSizeChanged ");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            Log.e("mijie", "onSurfaceTextureDestroyed ");
            if (mCamera == null) {
                return false;
            }

            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            Log.e("mijie", "onSurfaceTextureUpdated ");
        }
    }

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
            AppUtils.setCameraDisplayOrientation(this,id,mCamera);
            mCamera.addCallbackBuffer(mImageCallbackBuffer);

            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    if ((System.currentTimeMillis() - mTime) > TAKE_PIC_TIME) {
                        mTime = System.currentTimeMillis();
                        Log.e("liuwei", "TAKE_PIC ");
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

                    mCamera.addCallbackBuffer(bytes);
                }
            });
            opened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "fail to open camera");
            e.printStackTrace();
        }
        return opened;
    }

    private void disPalyPreview(SurfaceTexture surfaceTexture) {
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //调用startPreview来启动更新Preview Surface. 在拍照之前preview一定要启动
        mCamera.startPreview();
    }


    private void reDisPlayPreview() {
        mCamera.startPreview();
    }

    private void releaseCameraAndPreview() {
        //mPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            //调用`release()`释放Camera给其他应用程式。
            //程序应该在`onPause()`方法中立即释放camera,并在`onResume`中重新打开camera。
            mCamera.release();
            mCamera = null;
        }
    }

    private void closeCamera() {
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.stopPreview();
        mCamera.release();
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
            savePic(bmp);
            return null;
        }

        private void savePic(Bitmap bmp) {
            String fileSrc = null;
            if(null != bmp){
                FaceUtil.saveBitmapToFile(FaceDetectActivity.this, bmp);
            }

            // 获取图片保存路径
            fileSrc = FaceUtil.getImagePath(FaceDetectActivity.this);
            // 获取图片的宽和高
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            mImage = BitmapFactory.decodeFile(fileSrc, options);

            // 压缩图片
            options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
                    (double) options.outWidth / 1024f,
                    (double) options.outHeight / 1024f)));
            options.inJustDecodeBounds = false;
            mImage = BitmapFactory.decodeFile(fileSrc, options);


            // 若mImageBitmap为空则图片信息不能正常获取
            if(null == mImage) {
                showTip("图片信息无法正常获取！");
                return;
            }

            // 部分手机会对图片做旋转，这里检测旋转角度
            int degree = FaceUtil.readPictureDegree(fileSrc);
            if (degree != 0) {
                // 把图片旋转为正的方向
                mImage = FaceUtil.rotateImage(degree, mImage);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //可根据流量及网络状况对图片进行压缩
            mImage.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            mImageData = baos.toByteArray();
            onlineVerify();
        }
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
                String path = AppUtils.getAppPath()+ times + ".jpg";

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


    private void onlineVerify() {
        mAuthid = ((EditText) findViewById(R.id.online_authid)).getText().toString();
        if (TextUtils.isEmpty(mAuthid)) {
          //  showTip("authid不能为空");
            Log.e("liuwei","authid不能为空");
            return;
        }

        if (null != mImageData) {
            //mProDialog.setMessage("验证中...");
            Log.e("liuwei","验证中...");
            //mProDialog.show();
            // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
            // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。
            mFaceRequest.setParameter(SpeechConstant.AUTH_ID, mAuthid);
            mFaceRequest.setParameter(SpeechConstant.WFR_SST, "verify");
            mFaceRequest.sendRequest(mImageData, mRequestListener);
        } else {
           // showTip("请选择图片后再验证");
            Log.e("liuwei","请选择图片后再验证");
        }
    }

//    public static void saveBitmapToFile(Context context, Bitmap bmp) {
//        String file_path = AppUtils.getAppPath() + "hahah.jpg";
//        File file = new File(file_path);
//        FileOutputStream fOut;
//        try {
//            fOut = new FileOutputStream(file);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
//            fOut.flush();
//            fOut.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

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
                showTip("拍照失败，请重试");
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
            // 获取图片的宽和高
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            mImage = BitmapFactory.decodeFile(fileSrc, options);

            // 压缩图片
            options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
                    (double) options.outWidth / 1024f,
                    (double) options.outHeight / 1024f)));
            options.inJustDecodeBounds = false;
            mImage = BitmapFactory.decodeFile(fileSrc, options);


            // 若mImageBitmap为空则图片信息不能正常获取
            if (null == mImage) {
                showTip("图片信息无法正常获取！");
                return;
            }

            // 部分手机会对图片做旋转，这里检测旋转角度
            int degree = FaceUtil.readPictureDegree(fileSrc);
            if (degree != 0) {
                // 把图片旋转为正的方向
                mImage = FaceUtil.rotateImage(degree, mImage);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //可根据流量及网络状况对图片进行压缩
            mImage.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            mImageData = baos.toByteArray();

            // ((ImageView) findViewById(R.id.online_img)).setImageBitmap(mImage);
        }

    }

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

    private void showTip(final String str) {
        Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        toast.setText(str);
        toast.show();
    }



}
