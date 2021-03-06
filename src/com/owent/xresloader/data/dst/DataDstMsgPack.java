package com.owent.xresloader.data.dst;

import com.owent.xresloader.data.err.ConvException;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstMsgPack extends DataDstJava {

    private MessagePack msgpack = null;

    @Override
    public boolean init() {
        try {
            msgpack = new MessagePack();
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) throws ConvException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgpack.createPacker(out);

        DataDstJava.DataDstObject data_obj = build_data(desc);

        try {
            packer.write(data_obj.header);
            packer.write(data_obj.body);
        } catch (IOException e) {
            System.err.println("[ERROR] MessagePacker write failed.");
            e.printStackTrace();
        }
        // 带编码的输出
//        String encoding = SchemeConf.getInstance().getKey().getEncoding();
//        if (null == encoding || encoding.isEmpty())
//            return sb.toString().getBytes();
        return out.toByteArray();
    }

    @Override
    public final DataDstWriterNode compile() {
        System.err.println("[ERROR] msgpack can not be protocol description.");
        return null;
    }

    /**
     * 转储常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgpack.createPacker(out);

        try {
            packer.write(data);
        } catch (IOException e) {
            System.err.println("[ERROR] MessagePacker write failed.");
            e.printStackTrace();
        }
        // 带编码的输出
//        String encoding = SchemeConf.getInstance().getKey().getEncoding();
//        if (null == encoding || encoding.isEmpty())
//            return sb.toString().getBytes();
        return out.toByteArray();
    }
}
