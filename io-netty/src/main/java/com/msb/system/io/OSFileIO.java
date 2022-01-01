package com.bjmashibing.system.io;

import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class OSFileIO {

    static byte[] data = "123456789\n".getBytes();
    static String path = "/root/testfileio/out.txt";

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "0":
                testBasicFileIO();
                break;
            case "1":
                testBufferedFileIO();
                break;
            case "2":
                testRandomAccessFileWrite();
            case "3":
//                whatByteBuffer();
            default:

        }
    }

    //最基本的file写
    /*
    文件size增长很慢
    */
    public static void testBasicFileIO() throws Exception {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while (true) {
            Thread.sleep(10);
            out.write(data);
        }
    }

    //测试buffer文件IO
    //  jvm  8KB的数组   数组满了才调一次syscall  write(8KBbyte[])
    public static void testBufferedFileIO() throws Exception {
        File file = new File(path);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        while (true) {
            Thread.sleep(10);
            out.write(data);
        }
    }


    //测试 基于文件的NIO
    public static void testRandomAccessFileWrite() throws Exception {

        RandomAccessFile raf = new RandomAccessFile(path, "rw");

        raf.write("hello mashibing\n".getBytes());
        raf.write("hello seanzhou\n".getBytes());
        System.out.println("write------------");
        System.in.read();

        raf.seek(4);
        raf.write("ooxx".getBytes());

        System.out.println("seek---------");
        System.in.read();

        FileChannel rafchannel = raf.getChannel();
        /*
        只有文件FileChannel才有内存映射
         */
        //mmap  生成在 堆外  且和文件映射的 一个ByteBuffer  not  object
        MappedByteBuffer map = rafchannel.map(FileChannel.MapMode.READ_WRITE, 0, 4096);

        map.put("@@@".getBytes());  //不是系统调用  但是数据会到达 内核的pageCache
        //曾经我们是需要out.write()  这样的系统调用，才能让程序的data 进入内核的pageCache
        //曾经必须有用户态内核态切换
        //mmap的内存映射，依然是内核的pageCache体系所约束的！！！
        //换言之，丢数据
        //你可以去github上找一些 其他C程序员写的jni扩展库，使用linux内核的Direct IO
        //直接IO是忽略linux的pageCache
        //是把pageCache  交给了程序自己开辟一个字节数组当作pageCache，动用代码逻辑来维护一致性/dirty。。。一系列复杂问题

        System.out.println("map--put--------");
        System.in.read();

//        map.force(); //  flush

        raf.seek(0);

        ByteBuffer buffer = ByteBuffer.allocate(8192);
//        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        int read = rafchannel.read(buffer);   //buffer.put()
        System.out.println(buffer);
        buffer.flip();
        System.out.println(buffer);

        for (int i = 0; i < buffer.limit(); i++) {
            Thread.sleep(200);
            System.out.print(((char) buffer.get(i)));
        }
    }

    @Test
    public void whatByteBuffer() {

        // 从写到读 flip()
        // 从读到写 compact()

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        //ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        System.out.println("postition: " + buffer.position());
        System.out.println("limit: " + buffer.limit());
        System.out.println("capacity: " + buffer.capacity());
        System.out.println("mark: " + buffer);

        buffer.put("123".getBytes());

        System.out.println("-------------put:123......");
        System.out.println("mark: " + buffer);

        buffer.flip();   //读写交替

        System.out.println("-------------flip......");
        System.out.println("mark: " + buffer);

        buffer.put("hello".getBytes());
        System.out.println("-------------put again......");
        System.out.println("mark: " + buffer);

        buffer.get();

        System.out.println("-------------get......");
        System.out.println("mark: " + buffer);

        buffer.compact();
        /*
           public ByteBuffer compact() {
                System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
                position(remaining());
                limit(capacity());
                discardMark();
                return this;
            }
         */
        System.out.println("-------------compact......");
        System.out.println("mark: " + buffer);

        buffer.clear();

        System.out.println("-------------clear......");
        System.out.println("mark: " + buffer);

    }


}
