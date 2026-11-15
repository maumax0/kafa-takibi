package app.kafa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Kutuphanesiz en kucuk MQTT 3.1.1 yayinci (QoS 0). */
public class MqttPub {
    private Socket sock;
    private OutputStream out;

    public void connect(String host, int port, String clientId) throws IOException {
        sock = new Socket();
        sock.connect(new InetSocketAddress(host, port), 8000);
        sock.setTcpNoDelay(true);
        sock.setSoTimeout(8000);
        out = sock.getOutputStream();
        ByteArrayOutputStream v = new ByteArrayOutputStream();
        writeStr(v, "MQTT");
        v.write(4);      // protokol seviyesi 3.1.1
        v.write(2);      // temiz oturum
        v.write(0); v.write(60); // keepalive 60 sn
        writeStr(v, clientId);
        send(0x10, v.toByteArray());
        final InputStream in = sock.getInputStream();
        byte[] ack = new byte[4];
        int got = 0;
        while (got < 4) {
            int n = in.read(ack, got, 4 - got);
            if (n < 0) throw new IOException("broker baglantiyi kapatti");
            got += n;
        }
        if ((ack[0] & 0xF0) != 0x20 || ack[3] != 0) throw new IOException("broker reddetti");
        sock.setSoTimeout(0);
        Thread drain = new Thread(() -> {
            byte[] b = new byte[256];
            try { while (in.read(b) >= 0) { } } catch (Exception e) { }
        }, "mqtt-drain");
        drain.setDaemon(true);
        drain.start();
    }

    public void publish(String topic, byte[] payload) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        writeStr(b, topic);
        b.write(payload);
        send(0x30, b.toByteArray());
    }

    public void close() {
        try { if (out != null) { out.write(0xE0); out.write(0); out.flush(); } } catch (Exception e) { }
        try { if (sock != null) sock.close(); } catch (Exception e) { }
    }

    private synchronized void send(int header, byte[] body) throws IOException {
        out.write(header);
        int len = body.length;
        do {
            int d = len % 128;
            len /= 128;
            if (len > 0) d |= 128;
            out.write(d);
        } while (len > 0);
        out.write(body);
        out.flush();
    }

    private static void writeStr(ByteArrayOutputStream o, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        o.write(b.length >> 8);
        o.write(b.length & 255);
        o.write(b);
    }
}
