package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;

public class TCP_Receiver extends TCP_Receiver_ADT {
    private TCP_PACKET ackPack;    //回复的ACK报文段
    int sequence = 1;//用于记录当前待接收的包序号，注意包序号不完全是
    Window_Receiver receivewindow;

    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
        receivewindow = new Window_Receiver(client);
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        //检查校验码，生成ACK
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            //生成ACK报文段（设置确认号）
            tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            ackPack.setTcpH(tcpH);
            //回复ACK报文段
            reply(ackPack);
            // 加内容加到缓存区
            receivewindow.addRecvPacket(recvPack);
        } else {
            System.out.println("Recieve Computed: " + CheckSum.computeChkSum(recvPack));
            System.out.println("Recieved Packet" + recvPack.getTcpH().getTh_sum());
            System.out.println("Problem: Packet Number: " + recvPack.getTcpH().getTh_seq() + " + InnerSeq:  " + sequence);
        }
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //写入文件逻辑移入Window_Receiver
    }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte) 7);    //eFlag=0，信道无错误
        //发送数据报
        client.send(replyPack);
    }
}
