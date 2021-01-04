package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;

public class TCP_Sender extends TCP_Sender_ADT {
    private TCP_PACKET tcpPack;    //待发送的TCP数据报
    UDT_Timer timer;

    Window_Sender sendWindow;

    /*构造函数*/
    public TCP_Sender() {
        super();    //调用超类构造函数
        super.initTCP_Sender(this);        //初始化TCP发送端
        sendWindow = new Window_Sender(client);
    }

    @Override
    //可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
    public void rdt_send(int dataIndex, int[] appData) {

        //生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
        tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号：
        tcpS.setData(appData);
        tcpH.setTh_eflag((byte) 7);
        try {
            tcpPack = new TCP_PACKET(tcpH.clone(), tcpS.clone(), destinAddr);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
        tcpPack.setTcpH(tcpH);
        //发送TCP数据报
        try {
            this.sendWindow.rdt_send(tcpPack);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        //udt_send(tcpPack);
        while (!sendWindow.canContinue()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    //不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
    //逻辑已转入Window_Sender中Rdt_Send方法
    public void udt_send(TCP_PACKET stcpPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte) 7);
        //发送数据报
        client.send(stcpPack);
    }

    @Override
    //需要修改
    public void waitACK() {
        //等待ACK报文，已经转入其他方法
    }

    @Override
    //接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
    public void recv(TCP_PACKET recvPack) {
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            System.out.println("Receive ACK Number： " + recvPack.getTcpH().getTh_ack());
            sendWindow.recv(recvPack);
        } else {
            System.out.println("Receive Wrong ACK Number");
        }
        System.out.println();
    }
}
