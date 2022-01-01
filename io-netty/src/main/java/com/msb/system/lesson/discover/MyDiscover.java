package com.bjmashibing.system.lesson.discover;

import java.net.InetSocketAddress;
import java.util.HashMap;


public class MyDiscover {

    static HashMap<Class, InetSocketAddress> rpcMap = new HashMap<>();

    public static void register(Class interfacInfo,InetSocketAddress address){
        rpcMap.put(interfacInfo,address);
    }

    public static InetSocketAddress discover(Class interfaceInfo){
        return rpcMap.get(interfaceInfo);
    }






}
