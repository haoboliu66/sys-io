package com.bjmashibing.system.rpcdemo.rpc.protocol;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Myheader implements Serializable {
    //通信上的协议
    /*
    1，ooxx值
    2，UUID:requestID
    3，DATA_LEN

     */
    int flag;  //32bit可以设置很多信息。。。
    long requestID;
    long dataLen;


    public static Myheader createHeader(byte[] msg) {
        Myheader header = new Myheader();
        int size = msg.length;
        int f = 0x14141414;
        long requestID = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        //0x14  0001 0100
        header.setFlag(f);
        header.setDataLen(size);
        header.setRequestID(requestID);
        return header;
    }

}
