# 【007】Netty, ChannelPromise.md

- [Future和promise的原理](https://www.jianshu.com/p/5e3c1e5d95ac)

> 因为netty有很多异步操作，而为了管理异步操作我们需要提供监听者，而我们监听者需要根据我们操作的结果进行相应的操作。  
> 比如我们writeAndFlush的操作 我们想知道我们操作是否成功，完成还是失败等。  
> 那么我们操作成功之后通过promise的tryXX和setXX操作进行回调。  
> 而在这些回调过程中我们可能还需要用到我们的channel。  
> 所以这时候netty写了一个新的接口ChannelPromise即继承了channel接口和promise接口，
> 从而通过promise来实现回调，回调需要的channel从我们channel接口中获取。  
> netty有一个专门的实现类DefaultChannelPromise。