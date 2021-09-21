package com.hbliu;



public class TestSelector {


    public static void main(String[] args) {

//        ByteBuf buf = Unpooled.directBuffer(128);
//        System.out.println(buf.readableBytes());
//        System.out.println(buf.writableBytes());
//        buf.writeBytes(new byte[]{1, 2, 3});
//        byte[] dst = new byte[buf.readableBytes()];
//        ByteBuf buf1 = buf.readBytes(dst);
//        buf.discardReadBytes();
//        buf1 = Unpooled.copiedBuffer(new byte[]{65,65,66,68});
//        System.out.println(buf1.hasArray());
//        System.out.println(buf1.nioBuffer());
//
//        String s = buf1.toString(UTF_8);
//        System.out.println(s);

        System.out.println(Thread.currentThread().getContextClassLoader());
        System.out.println(Thread.currentThread().getContextClassLoader().getParent());
        System.out.println(Thread.currentThread().getContextClassLoader().getParent().getParent());
    }
}

