package com.bjmashibing.system.io.netty;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class HLNetty {

       @Test
    public void myBytebuf() throws IOException {

           Selector selector = Selector.open();
           SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("192.168.2.88", 9090));
           SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);

//
//           ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20);
//        System.out.println(buf.getClass());
//
//        print(buf);
//        buf.writeBytes(new byte[]{1, 2, 3, 4});
//        print(buf);
//        buf.writeBytes(new byte[]{1, 2, 3, 4});
//        print(buf);
//        buf.writeBytes(new byte[]{1, 2, 3, 4});
//        print(buf);
//        buf.writeBytes(new byte[]{1, 2, 3, 4});
//        print(buf);
//        buf.writeBytes(new byte[]{1, 2, 3, 4});
//        print(buf);
        //buf.writeBytes(new byte[]{1, 2, 3, 4});
    }

    public static void print(ByteBuf buf) {
        System.out.println("isReadable() " + buf.isReadable());
        System.out.println("isWritable() " + buf.isWritable());
        System.out.println("writerIndex() " + buf.writerIndex());
        System.out.println("readerIndex() " + buf.readerIndex());
        System.out.println("------------------");
    }

}
