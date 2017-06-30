package com.mj.voicerecoder.bean;

import java.util.List;

/**
 * Created by liuwei on 6/30/17.
 */

public class JpushBean {

    private int type;
    private List<String> content;

    public void setType(int type) {
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
