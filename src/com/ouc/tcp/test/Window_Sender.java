package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.*;
import java.util.logging.Logger;

public class Window_Sender {
    class Window {
        boolean ack;
        long startSendTime; //开始发包时间
        TCP_PACKET packet;

        Window(TCP_PACKET packet) {
            this.packet = packet;
        }
    }

    public static long TIMEOUTTIME = 3000; // 超时时间
    Logger logger;
    List<Window> sendContent;
    int startWindowIndex; // 窗口头
    int endWindowIndex; // 窗口尾
    int windowSize = 100; //暂时固定窗口大小
    Client client;

    public boolean canContinue() {
        int num = this.startWindowIndex + this.windowSize - this.endWindowIndex;
        return num > 0;
    }

    public Window_Sender(Client client) {
        logger = Logger.getLogger("RDTSender");
        this.client = client;
        sendContent = new ArrayList<Window>();
        waitOvertime();
    }

    //发包重新封装
    public void rdt_send(TCP_PACKET packet) throws CloneNotSupportedException {
        Window window = addPacket(packet.clone());
        sendWindow(window, true);
    }

    public Window addPacket(TCP_PACKET packet) {
        Window window = new Window(packet);
        sendContent.add(window);
        endWindowIndex++;
        return window;
    }

    private void sendWindow(Window window, boolean isFirst) {
        //发送数据报
        window.startSendTime = System.currentTimeMillis();
        client.send(window.packet);
        if (!isFirst) logger.warning("重新发送包:" + window.packet.getTcpH().getTh_seq());
    }

    public void waitOvertime() {
        TimerTask dealOverTime = new TimerTask() {
            @Override
            public void run() {
                int index = startWindowIndex;
                boolean updateStart = true;
                Window window;
                while (index < endWindowIndex) {
                    // 如果第index个包超时了
                    window = sendContent.get(index);
                    if (updateStart && window.ack) startWindowIndex = index + 1;
                    else if (!window.ack) {
                        updateStart = false;
                        //没有收到ack,则尝试重发
                        if (TIMEOUTTIME < (System.currentTimeMillis() - window.startSendTime))
                            sendWindow(window, false);
                    }
                    index++;
                }
            }
        };
        //设置计时器和超时重传任务
        UDT_Timer timer = new UDT_Timer();
        timer.schedule(dealOverTime, 0, 200);
    }

    public void recv(TCP_PACKET recvPack) {
        int ack = recvPack.getTcpH().getTh_ack();
        Window window;
        boolean canUpdate = true;
        int seq;
        for (int i = startWindowIndex; i < endWindowIndex; i++) {
            window = sendContent.get(i);
            seq = window.packet.getTcpH().getTh_seq();
            if (seq <= ack) {
                if (canUpdate && window.ack) startWindowIndex = i + 1;
                else canUpdate = false;
                if (seq == ack) {
                    if (window.ack) logger.info("重复接收到ack:" + ack + " index为:" + i + "的窗口块已经ack");
                    window.ack = true;
                    break;
                }
            } else break;
        }
    }
}