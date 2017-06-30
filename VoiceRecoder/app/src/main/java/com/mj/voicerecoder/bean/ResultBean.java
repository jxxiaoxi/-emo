package com.mj.voicerecoder.bean;

/**
 * Created by liuwei on 6/17/17.
 */

public class ResultBean {


    /**
     * errorCode : 0
     * returnObject : true
     * message : null
     */

    private int errorCode;
    private boolean returnObject;
    private Object message;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isReturnObject() {
        return returnObject;
    }

    public void setReturnObject(boolean returnObject) {
        this.returnObject = returnObject;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }
}
