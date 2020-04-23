package com.vergilyn.examples.simplex;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Simplex;
import static com.vergilyn.examples.common.NettyConstants.newClientBootstrap;
import static com.vergilyn.examples.common.NettyConstants.printf;
import static com.vergilyn.examples.common.NettyConstants.println;
import static com.vergilyn.examples.common.NettyConstants.sleep;

/**
 * @author VergiLyn
 * @date 2019-04-15
 */
@Slf4j
public class SimplexNettyClient {
    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = newClientBootstrap(1, Simplex);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {

                        println("client initChannel() before");
                        sleep(10, TimeUnit.SECONDS, "initChannel() before");

                        ch.pipeline()
                                .addLast("frame-decoder", new LineBasedFrameDecoder(100))  // inbound
                                .addLast("encoder", new StringEncoder())  // outbound
                                .addLast("decoder", new StringDecoder());  // inbound

                        println("client initChannel() after");
                    }
                });

        ChannelFuture channelFuture = bootstrap.connect(INET_HOST, Simplex.inetPort);
        sleep(0, TimeUnit.SECONDS, "bootstrap#connect()");

        Channel channel = channelFuture.channel();
        sleep(0, TimeUnit.SECONDS, "channelFuture#channel()");

        while (true) {

            String msg = String.format("client send: %s \r\n", LocalTime.now().toString());
            /* 可以完全避免 第1次传输出现 "unsupported message type"
             * ByteBuf byteBuf = Unpooled.wrappedBuffer(msg.getBytes());
             * ChannelFuture future = channel.writeAndFlush(byteBuf);
             */
            // ChannelFuture future = channel.pipeline().writeAndFlush(msg);  // -> tail.writeAndFlush(msg)
            ChannelFuture future = channel.write(msg);
            printf("writeAndFlush >>>> %s", msg);

            future.addListener((ChannelFutureListener) f
                    -> printf("client future: %b, cause: %s \r\n", f.isSuccess(), f.cause()));

            TimeUnit.SECONDS.sleep(10);
        }
    }


}
