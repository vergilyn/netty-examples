package com.vergilyn.examples.simplex;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Simplex;
import static com.vergilyn.examples.common.NettyConstants.newClientBootstrap;

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
                        ch.pipeline()
                                .addLast("encoder", new StringEncoder())  // outbound
                                .addLast("decoder", new StringDecoder());  // inbound
                    }
                });

        Channel channel = bootstrap.connect(INET_HOST, Simplex.inetPort).channel();

        while (true) {
            String msg = String.format("client send: %s", LocalTime.now().toString());
            ChannelFuture future = channel.writeAndFlush(msg);
            System.out.println(msg);

            future.addListener((ChannelFutureListener) f
                    -> System.out.printf("client future: %b, cause: %s \r\n", f.isSuccess(), f.cause()));

            TimeUnit.SECONDS.sleep(10);
        }
    }
}
