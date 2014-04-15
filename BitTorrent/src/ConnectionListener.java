
import com.turn.ttorrent.common.Torrent;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

//import com.turn.ttorrent.client.ConnectionHandler;
//import com.turn.ttorrent.client.IncomingConnectionListener;


public class ConnectionListener implements Runnable {

	private Torrent torrent;
	private InetSocketAddress address;
	private String id;
	private ServerSocketChannel channel;
	private boolean stop;
	
	private ExecutorService executor;
	private Thread thread;
	
	
	ConnectionListener(Torrent torrent, String id, InetAddress address){
		this.torrent = torrent;
		this.id=id;
		
		for (int port = 6881; port <= 6889; port++) {
			InetSocketAddress tryAddress =
				new InetSocketAddress(address, port);

			try {
				this.channel = ServerSocketChannel.open();
				this.channel.socket().bind(tryAddress);
				this.channel.configureBlocking(false);
				this.address = tryAddress;
				break;
			} catch (IOException ioe) {
				System.out.println("Could not bind to "+port+" trying next one");
			}
		}

		/*if (this.channel == null || !this.channel.socket().isBound()) {
			throw new IOException("No available port for the BitTorrent client!");
		}*/

		System.out.println("Listening for incoming connections on something.");
		
	}

	public void start() {
		if (this.channel == null) {
			throw new IllegalStateException(
				"Connection handler cannot be recycled!");
		}

		this.stop = false;

		if (this.executor == null || this.executor.isShutdown()) {
			this.executor = new ThreadPoolExecutor(
				20, 20, 10, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		}

		if (this.thread == null || !this.thread.isAlive()) {
			this.thread = new Thread(this);
			this.thread.setName("bt-serve");
			this.thread.start();
		}
	}

	@Override
	public void run() {
		while (!this.stop) {
			try {
				SocketChannel client = this.channel.accept();
				if (client != null) {
					this.performHandshake(client);
				}
			} catch (SocketTimeoutException ste) {
				// Ignore and go back to sleep
			} catch (IOException ioe) {
				System.out.println("error in connection");
				this.stop();
			}

			/*try {
				Thread.sleep(100);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}*/
		}
	}
	
	private void performHandshake(SocketChannel client)
		throws IOException, SocketTimeoutException {
		System.out.println("new connection, waiting for handshake");
		try {
			Handshake handshake = getHandshake(client, null);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		sendHandshake(client);
	}
	
	public void stop() { //copied
		this.stop = true;

		/*if (this.thread != null && this.thread.isAlive()) {
			try {
				this.thread.join();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}

		if (this.executor != null && !this.executor.isShutdown()) {
			this.executor.shutdownNow();
		}

		this.executor = null;
		this.thread = null;*/
	}
	
	public InetSocketAddress getAddress() {
		return this.address;
	}
	
	public Handshake getHandshake(SocketChannel channel, byte[] peerId) throws IOException, ParseException{
		ByteBuffer hs = ByteBuffer.allocate(Handshake.LENGTH); //byte size of handshake
		channel.read(hs);
		return Handshake.parse(hs);
	}
	
	private int sendHandshake(SocketChannel channel) throws IOException {
		Handshake hs = Handshake.make(this.torrent.getInfoHash(), 
				this.id.getBytes(Torrent.BYTE_ENCODING));
		return channel.write(hs.getDataBytes());
	}
}
