package com.bjmashibing.system.rpcdemo.service;

public class MyCar implements Car {

    @Override
    public String ooxx(String msg) {
        return "server res " + msg;
    }

    @Override
    public Person oxox(String name, Integer age) {
        Person p = new Person();
        p.setName(name);
        p.setAge(age);
        return p;
    }
}
