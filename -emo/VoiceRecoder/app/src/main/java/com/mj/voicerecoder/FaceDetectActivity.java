package com.mj.voicerecoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.mj.voicerecoder.base.BaseActivity;
import com.mj.voicerecoder.util.AppUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by liuwei on 6/24/17.
 * 用于人脸识别
 */

public class FaceDetectActivity extends BaseActivity {
    @BindView(R.id.surface_view)
    TextureView surfaceView;
    @BindView(R.id.bt_register)
    Button btRegister;
    @BindView(R.id.bt_detect)
    Button btDetect;

    private Camera mCamera;
    private static final int YUV_1080_SIZE = 3110400;
    private byte[] mImageCallbackBuffer = new byte[YUV_1080_SIZE];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        ButterKnife.bind(this);
        surfaceView.setSurfaceTextureListener(new CoustomFaceViewListener());
        openCamera(1);
    }

    @Override
    public boolean showActionBar() {
        return true;
    }

    @OnClick({R.id.bt_register, R.id.bt_detect})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.bt_register:
                Log.e("mijie", "bt_register ");
                takePhoto();
                break;
            case R.id.bt_detect:
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class CoustomFaceViewListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e("mijie", "onSurfaceTextureAvailable ");
            if (mCamera != null) {
                try {
                    mCamera.setPreviewTexture(surfaceTexture);
                    mCamera.startPreview();
                } catch (IOException ioe) {
                    ioe.toString();
                }
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

    private void closeCamera() {
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.stopPreview();
        mCamera.release();
    }

    private void openCamera(int num) {
        Log.e("mijie", "camera num :  " + num);
        mCamera = Camera.open(num);
        mCamera.setDisplayOrientation(90);
        mCamera.addCallbackBuffer(mImageCallbackBuffer);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {


                mCamera.addCallbackBuffer(bytes);
            }
        });
    }


    private class FaceTask extends AsyncTask<Void, Void, Void> {
        private byte[] mData;
        //构造函数
        FaceTask(byte[] data){
            this.mData = data;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }

    private void takePhoto() {
        if (mCamera == null)
            return;

        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                FileOutputStream outSteam = null;
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                String times = format.format((new Date()));
                String path = AppUtils.getAppPath()+times+ ".jpg";
                try {

                    File file = new File(path);
                    Log.e("liuwei","xxxx "+file.getParentFile().exists());
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    outSteam = new FileOutputStream(path);
                    outSteam.write(bytes);
                    outSteam.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

}
