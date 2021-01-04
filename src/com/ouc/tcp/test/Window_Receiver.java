package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Window_Receiver {
    class Window {
        TCP_PACKET packet;

        Window(TCP_PACKET packet) {
            this.packet = packet;
        }
    }

    SortedSet<Window> recvContent;
    Client client;
    int lastSaveSeq = -1; //上个包seq
    int lastLength = 0; //上个包长度

    public Window_Receiver(Client client) {
        this.client = client;

        recvContent = new TreeSet<Window>(new Comparator<Window>() {
            @Override
            public int compare(Window o1, Window o2) {
                return o1.packet.getTcpH().getTh_seq() - o2.packet.getTcpH().getTh_seq();
            }
        });
    }

    public void addRecvPacket(TCP_PACKET packet) {
        // 判断是否有序
        int seq = packet.getTcpH().getTh_seq();
        if ((seq == lastSaveSeq + lastLength) || lastSaveSeq == -1) {
            lastLength = packet.getTcpS().getData().length;
            lastSaveSeq = seq;
            waitWrite(packet);
        } else if (seq > lastSaveSeq) {
            System.out.println("失序接收，缓存seq:" + seq + "到列表,last is:" + lastSaveSeq);
            recvContent.add(new Window(packet)); //缓存在有序集合中
        }
    }

    public void waitWrite(TCP_PACKET packet) {
        int seq;
        File fw = new File("recvData.txt");
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(fw, true));
            Window window;
            int[] data = packet.getTcpS().getData();
            for (int i : data) writer.write(i + "\n");
            writer.flush();
            Iterator<Window> it = recvContent.iterator();
            // 在缓存队列里看是否还有有序的包,一起向上递交
            while (it.hasNext()) {
                window = it.next();
                seq = window.packet.getTcpH().getTh_seq();
                data = window.packet.getTcpS().getData();
                if (seq == lastSaveSeq + lastLength) {  // 判断是否有序
                    lastLength = packet.getTcpS().getData().length;
                    lastSaveSeq = seq;
                    for (int i : data) writer.write(i + "\n");
                    writer.flush();
                    it.remove();
                } else break;
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}