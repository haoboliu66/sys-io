package com.bjmashibing.system.rpcdemo.rpc.transport;

import com.bjmashibing.system.rpcdemo.rpc.ResponseMappingCallback;
import com.bjmashibing.system.rpcdemo.util.Packmsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.java.Log;

@Log
public class ClientResponseHandler extends ChannelInboundHandlerAdapter {

    //consumer.....
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("ClientResponses - client channel read.....");
        Packmsg responsepkg = (Packmsg) msg;

        //曾经我们没考虑返回的事情
        ResponseMappingCallback.runCallBack(responsepkg);

    }
}

