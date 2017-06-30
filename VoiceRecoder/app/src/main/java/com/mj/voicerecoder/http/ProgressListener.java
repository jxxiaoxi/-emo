package com.mj.voicerecoder.http;

/**
 * Created by liuwei on 6/29/17.
 */

public interface ProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}
