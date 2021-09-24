package com.bjmashibing.system.rpcdemo.rpc.protocol;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
public class MyContent implements Serializable {

    private String name;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] args;
    //返回的数据
    private Object result;


}
