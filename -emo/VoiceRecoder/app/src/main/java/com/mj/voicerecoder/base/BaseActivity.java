package com.mj.voicerecoder.base;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by liuwei on 6/21/17.
 */

public class BaseActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private PermissionCallback permissionRunnable;
    private static final int RC_LOCATION_CONTACTS_PERM = 666;
    private int mPermissionLength;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(showActionBar()) {
            getSupportActionBar().hide();
        }
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //全屏显示
    }

    public boolean showActionBar(){
        return false;
    }

    public interface PermissionCallback {
        void gainPermissionSuccess();

        void gainPermissionFail();
    }


    /**
     * @param permissionDes 权限第一次被拒绝后，再次进入app，提示用户还有权限没有打开，要正确使用ａｐｐ还需打开的，提示语
     * @param runnable      　回调
     * @param perms         　权限组
     */
    public void gainPermission(String permissionDes, PermissionCallback runnable, String[] perms) {
        this.permissionRunnable = runnable;
        EasyPermissions.requestPermissions(this, permissionDes,
                RC_LOCATION_CONTACTS_PERM, perms);
        mPermissionLength = perms.length;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 调用EasyPermissions的onRequestPermissionsResult方法，参数和系统方法保持一致，然后就不要关心具体的权限申请代码了
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int i, List<String> list) {
        Log.e("mijie","onPermissionsGranted mPermissionLength "+mPermissionLength+ "     ;list.size  : "+list.size());
        if(mPermissionLength == list.size()){
            permissionRunnable.gainPermissionSuccess();
        }else{
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onPermissionsDenied(int i, List<String> list) {
            permissionRunnable.gainPermissionFail();
    }


    public boolean hasPermissions(String[] perms) {
        return EasyPermissions.hasPermissions(this, perms);
    }
}
