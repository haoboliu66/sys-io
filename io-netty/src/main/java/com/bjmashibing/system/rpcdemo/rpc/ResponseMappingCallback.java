package com.bjmashibing.system.rpcdemo.rpc;

import com.bjmashibing.system.rpcdemo.util.Packmsg;
import lombok.extern.java.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Log
public class ResponseMappingCallback {
    static ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<>();

    public static void addCallBack(long requestID, CompletableFuture cb) {
        mapping.putIfAbsent(requestID, cb);
    }

    public static void runCallBack(Packmsg msg) {
        CompletableFuture cf = mapping.get(msg.getHeader().getRequestID());
//        runnable.run();
        cf.complete(msg.getContent().getResult());
        removeCB(msg.getHeader().getRequestID());
    }

    private static void removeCB(long requestID) {
        mapping.remove(requestID);
    }

}

