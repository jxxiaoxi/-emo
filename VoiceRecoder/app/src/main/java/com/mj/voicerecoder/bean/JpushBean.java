package com.mj.voicerecoder.bean;

import java.util.List;

/**
 * Created by liuwei on 6/30/17.
 */

public class JpushBean {

    /**
     * type : 1
     * content : ["http://www.lamago.net/mijiemonitor/upload/0220170630104114272147.jpeg","http://www.lamago.net/mijiemonitor/upload/0220170630104331752659.jpeg"]
     */

    private int type;
    private List<String> content;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }
}
