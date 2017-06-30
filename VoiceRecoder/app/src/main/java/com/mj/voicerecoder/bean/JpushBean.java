package com.mj.voicerecoder.bean;

import android.util.Log;

import java.util.List;

/**
 * Created by liuwei on 6/30/17.
 */

public class JpushBean {

    /**
     * type : 1
     * content : ["http://www.lamago.net/mijiemonitor/upload/0220170630103539870074.jpeg"]
     */

    private int type;
    private List<String> content;

    public void setType(int type) {
        Log.e("mijie","setType : "+type);
        this.type = type;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public List<String> getContent() {
        return content;
    }
}
