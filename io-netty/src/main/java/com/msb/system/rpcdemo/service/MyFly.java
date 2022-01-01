package com.bjmashibing.system.rpcdemo.service;

public class MyFly implements Fly {

    @Override
    public void xxoo(String msg) {
        System.out.println("server,get client arg:" + msg);
    }
}
