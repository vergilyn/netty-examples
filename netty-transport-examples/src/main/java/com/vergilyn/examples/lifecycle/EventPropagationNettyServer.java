package com.vergilyn.examples.lifecycle;

import java.net.SocketAddress;
import java.time.LocalTime;
import java.util.List;

import com.google.common.collect.Lists;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.EventPropagation;
import static com.vergilyn.examples.common.NettyConstants.newServerBootstrap;

/**
 * @author vergilyn
 * @date 2020-04-01
 */
@Slf4j
public class EventPropagationNettyServer {

    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = newServerBootstrap(1, 1, EventPropagation);

        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("decoder", new StringDecoder())  // inbound
                                .addLast("encoder", new StringEncoder())  // outbound
                                .addLast("inbound-01", new PropagationInboundHandler("server"))
                                .addLast("outbound-01", new PropagationOutBoundHandler("server"))
                                .addLast("outbound-02", new PropagationOutBoundHandler("server"))
                                .addLast("inbound-02", new PropagationInboundHandler("server"));
                    }
                })
                .bind(EventPropagation.inetPort);
    }

    private static void printMethodName(ChannelHandlerContext ctx, String name, String methodName){
        System.out.printf("> %s, %s, %s, %s \r\n", ctx.name(), name, methodName, LocalTime.now().toString());
        // log.info("method: {}", methodName);
    }

    @ChannelHandler.Sharable
    static class PropagationInboundHandler extends ChannelInboundHandlerAdapter {
        public static final List<PropagationInboundHandler> THIS_LIST = Lists.newArrayList();
        private String name;

        public PropagationInboundHandler(){

        }

        public PropagationInboundHandler(String name){
            this.name = name;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            // pipeline中的新节点添加完成，于是便开始回调用户代码
            // 节点被添加完毕之后回调到此
            printMethodName(ctx, this.name, "handlerAdded()");
            THIS_LIST.add(this);
            super.handlerAdded(ctx);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            // 节点被删除完毕之后回调到此，可做一些资源清理
            // 另外，被删除的节点因为没有对象引用到，过段时间就会被gc自动回收
            printMethodName(ctx, this.name, "handlerRemoved()");
            super.handlerRemoved(ctx);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "channelRegistered()");
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "channelUnregistered");
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "channelActive()");
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "channelInactive()");
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            printMethodName(ctx, this.name, "channelRead()");

            System.out.println("receive message: " + msg);

            // 最后一个 inbound，往 outbound 传递
            if (THIS_LIST.get(THIS_LIST.size() - 1) == this){
               ctx.writeAndFlush(msg);
               return;
            }

            // super.channelRead(ctx, msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "channelReadComplete()");
            super.channelReadComplete(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            printMethodName(ctx, this.name, "userEventTriggered()");
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "channelWritabilityChanged()");
            super.channelWritabilityChanged(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            printMethodName(ctx, this.name, "exceptionCaught()");
            cause.printStackTrace();
        }
    }

    @ChannelHandler.Sharable
    static class PropagationOutBoundHandler extends ChannelOutboundHandlerAdapter {
        private String name;

        public PropagationOutBoundHandler(){

        }

        public PropagationOutBoundHandler(String name){
            this.name = name;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "handlerAdded()");
            super.handlerAdded(ctx);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "handlerRemoved()");
            super.handlerRemoved(ctx);
        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            printMethodName(ctx, this.name, "bind()");
            super.bind(ctx, localAddress, promise);
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            printMethodName(ctx, this.name, "connect()");
            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            printMethodName(ctx, this.name, "disconnect()");
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            printMethodName(ctx, this.name, "close()");
            super.close(ctx, promise);
        }

        @Override
        public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            printMethodName(ctx, this.name, "deregister()");
            super.deregister(ctx, promise);
        }

        @Override
        public void read(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "read()");

            // important >>>> 继续往后传递
            // ChannelOutboundHandler可以通过read()方法 **在必要的时候阻止向inbound读取更多数据的操作。**这个设计在处理协议的握手时非常有用。
            // super.read(ctx);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            printMethodName(ctx, this.name, "write()");

            super.write(ctx, msg, promise);

        }

        @Override
        public void flush(ChannelHandlerContext ctx) throws Exception {
            printMethodName(ctx, this.name, "flush()");
            super.flush(ctx);
        }
    }
}
