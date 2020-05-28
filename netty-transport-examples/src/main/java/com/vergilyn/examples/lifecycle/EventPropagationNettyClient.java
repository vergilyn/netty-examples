package com.vergilyn.examples.lifecycle;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.string.StringEncoder;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.EventPropagation;
import static com.vergilyn.examples.common.NettyConstants.newClientBootstrap;

/**
 * @author vergilyn
 * @date 2020-04-01
 */
public class EventPropagationNettyClient {

    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = newClientBootstrap(1, EventPropagation);

        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast("encoder", new StringEncoder())
                        .addLast("inbound-01", new EventPropagationNettyServer.PropagationInboundHandler("server"))
                        .addLast("outbound-01", new EventPropagationNettyServer.PropagationOutBoundHandler("server"));
            }
        });

        Channel channel = bootstrap.connect(INET_HOST, EventPropagation.inetPort).channel();
        System.out.println("client connected: " + LocalTime.now().toString());
        TimeUnit.SECONDS.sleep(2);

        sendMessage(0, channel);

        TimeUnit.SECONDS.sleep(10);
        System.out.println("client disconnect: " + LocalTime.now().toString());
        channel.deregister();

//        TimeUnit.SECONDS.sleep(10);
//        channel = bootstrap.connect(INET_HOST, EventInvokeSequence.inetPort).channel();
//        sendMessage(1, channel);


//        channel.close();
//        channel.deregister();
    }

    private static void sendMessage(int index, Channel channel){
        String msg = String.format("[%d][%s]", index, LocalTime.now().toString());
        channel.writeAndFlush(msg);
        System.out.println("client send: " + msg);
    }
}
