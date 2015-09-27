package nio.springserver.listener;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;

/**
 * Sends a sequence of integers to a {@link FactorialServer} to calculate the
 * factorial of the specified integer.
 */
public final class FactorialClient {

	static final boolean SSL = System.getProperty(
	    "ssl") != null;
	static final String HOST = System.getProperty(
	    "host",
	    "127.0.0.1");
	static final int PORT = Integer.parseInt(
	    System.getProperty(
	        "port",
	        "8322"));
	static final int COUNT = Integer.parseInt(
	    System.getProperty(
	        "count",
	        "1000"));

	public static void main(String[] args)
	    throws Exception {
		// Configure SSL.
		final SslContext sslCtx = null;

		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(
			    group)
			        .channel(
			            NioSocketChannel.class)
			        .handler(
			            new FactorialClientInitializer(
			                sslCtx));

			// Make a new connection.
			ChannelFuture f = b.connect(
			    HOST,
			    PORT).sync();

			// Get the handler instance to retrieve the answer.
			FactorialClientHandler handler = (FactorialClientHandler) f.channel().pipeline().last();

			// Print out the answer.
			System.err.format(
			    "Factorial of %,d is: %,d",
			    COUNT,
			    handler.getFactorial());
		} finally {
			group.shutdownGracefully();
		}
	}
}