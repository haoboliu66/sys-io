package com.bjmashibing.system.lesson.rpcenv.connectpool;

import com.bjmashibing.system.lesson.rpcenv.PackageMsg;
import com.bjmashibing.system.lesson.packmode.MsgCallBackMapping;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author: 马士兵教育
 * @create: 2020-07-18 11:30
 */
public class ClientResponsesHandler extends ChannelInboundHandlerAdapter {

    //consumer.....
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PackageMsg pkg = (PackageMsg) msg;
        MsgCallBackMapping.runCallBack(pkg);
    }
}
