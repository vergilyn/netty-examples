package com.vergilyn.examples.netty.performance.write;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Write;
import static com.vergilyn.examples.common.NettyConstants.newClientBootstrap;

/**
 * 测试同一个channel，第1次 write 与 第2+次 执行时间消耗的差距
 * @author vergilyn
 * @date 2020-04-23
 */
public class WriteNettyClient {
    private static final NumberFormat FORMATTER = new DecimalFormat("###,###");

    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = newClientBootstrap(1, Write);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast("frame-decoder", new LineBasedFrameDecoder(100))  // inbound
                        .addLast("encoder", new StringEncoder())  // outbound
                        .addLast("decoder", new StringDecoder());  // inbound
            }
        });

        ChannelFuture channelFuture = bootstrap.connect(INET_HOST, Write.inetPort);
        Channel channel = channelFuture.channel();

        TimeUnit.SECONDS.sleep(1);  // promise channel/pipeline right!

        /*
         * [01] write elapsed >>>> 20,205,200 ns
         * [02] write elapsed >>>> 340,700 ns
         * [03] write elapsed >>>> 348,400 ns
         * [04] write elapsed >>>> 337,400 ns
         * [05] write elapsed >>>> 288,200 ns
         * [06] write elapsed >>>> 298,900 ns
         * [07] write elapsed >>>> 1,658,000 ns
         * [08] write elapsed >>>> 418,500 ns
         * [09] write elapsed >>>> 531,100 ns
         * [10] write elapsed >>>> 460,100 ns
         */
        for (int i = 1, len = 20; i <= len; i++) {
            long begin = System.nanoTime();
            String msg = String.format("client send: %s \r\n", LocalTime.now().toString());
            ChannelFuture future = channel.writeAndFlush(msg);

            System.out.printf("[%02d] write elapsed >>>> %s ns \r\n", i, FORMATTER.format(System.nanoTime() - begin));

            /*future.addListener((ChannelFutureListener) f
                    -> printf("client future: %b, cause: %s \r\n", f.isSuccess(), f.cause()));*/

            TimeUnit.SECONDS.sleep(1);
        }

        channel.close();
    }


}
