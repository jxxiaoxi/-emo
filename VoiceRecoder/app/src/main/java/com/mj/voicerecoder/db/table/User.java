package com.mj.voicerecoder.db.table;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by liuwei on 6/17/17.
 */

@Entity
public class User {

    @Id
    private long id;
    private String name;

    @Generated(hash = 586692638)
    public User() {
    }

    @Generated(hash = 1144922831)
    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}