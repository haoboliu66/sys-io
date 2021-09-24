package com.bjmashibing.system.rpcdemo.util;

import com.bjmashibing.system.rpcdemo.rpc.protocol.MyContent;
import com.bjmashibing.system.rpcdemo.rpc.protocol.Myheader;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Packmsg {

    private Myheader header;
    private MyContent content;



}
