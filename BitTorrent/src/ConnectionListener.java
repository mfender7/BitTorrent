
import com.turn.ttorrent.common.Torrent;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.text.ParseException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;


//import com.turn.ttorrent.client.ConnectionHandler;
//import com.turn.ttorrent.client.IncomingConnectionListener;


public class ConnectionListener implements Runnable {

	private Torrent torrent;
	private InetSocketAddress address;
	private String id;
	private ServerSocketChannel channel;
	private ServerSocket ssocket;
	private boolean stop;
	private int port;
	private Socket socket;
	
	private ExecutorService executor;
	private Thread thread;
	
	
	ConnectionListener(Torrent torrent, String id, InetAddress address) throws IOException{
		this.torrent = torrent;
		this.id=id;
		this.ssocket = new ServerSocket();
		
		for (int port = 6881; port <= 6889; port++) {
			InetSocketAddress tryAddress =
				new InetSocketAddress(address, port);

			try {
				this.ssocket.bind(tryAddress);
				this.address = tryAddress;
				this.port=port;
				break;
			} catch (IOException ioe) {
				System.out.println("Could not bind to "+port+" trying next one");
			}
		}

		System.out.println("Listening for incoming connections on something.");
		
	}

	public void start() {
		if (!this.ssocket.isBound()) {
            throw new IllegalStateException("Can't start ConnectionHandler " +
                            "without a bound socket!");
		}

		this.stop = false;
		System.out.println("ConnectionListener started");

		if (this.thread == null || !this.thread.isAlive()) {
			this.thread = new Thread(this);
			this.thread.setName("bt-serve");
			this.thread.start();
		}
	}

	@Override
	public void run() {
		 try {
             this.ssocket.setSoTimeout(250);
		 } catch (SocketException se) {
             //logger.warn("{}", se);
             this.stop();
		 }
/*
		 while (!this.stop) {
             try {
                 this.accept();
             } catch (SocketTimeoutException ste) {
                     // Ignore and go back to sleep
             } catch (IOException ioe) {
                     //logger.warn("{}", ioe);
                     this.stop();
             }

             try {
                     Thread.sleep(750);
             } catch (InterruptedException ie) {
                     // Ignore
             }
		 }*/

		 try {
             this.ssocket.close();
		 } catch (IOException ioe) {
             // Ignore
		 }
	}
	
	private void accept() throws IOException, SocketTimeoutException {
        Socket socket = this.ssocket.accept();

        try {
                //logger.debug("New incoming connection ...");
                Handshake hs = this.getHandshake(socket, null);
                this.sendHandshake(socket);
                //this.fireNewPeerConnection(socket, hs.getPeerId());
        } catch (ParseException pe) {
                System.out.println(pe.getMessage());
                try { socket.close(); } catch (IOException e) { }
        } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
                try {
                        if (!socket.isClosed()) {
                                socket.close();
                        }
                } catch (IOException e) {
                        // Ignore
                }
        }
	}

	
	public void stop() { //copied
		this.stop = true;

        if (this.thread != null && this.thread.isAlive()) {
                this.thread.interrupt();
        }

        this.thread = null;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public InetSocketAddress getAddress() {
		return this.address;
	}
	

	private Handshake getHandshake(Socket socket, byte[] peerId)
			throws IOException, ParseException {
		// Read the handshake from the wire
		System.out.println("in getHandshake");
		InputStream input = socket.getInputStream();
		int pstrlen = input.read();
        byte[] data = new byte[Handshake.LENGTH];
        data[0] = (byte)pstrlen;
        input.read(data, 1, data.length-1);

        // Parse and check the handshake
        Handshake hs = Handshake.parse(ByteBuffer.wrap(data));
        if (!Arrays.equals(hs.getInfoHash(), this.torrent.getInfoHash())) {
            System.out.println("Nuuuu");
        	/*throw new ParseException("Handshake for unknow torrent " +
                                Torrent.byteArrayToHexString(hs.getInfoHash()) +
                                " from " + this.socketRepr(socket) + ".", pstrlen + 9);*/
        }

        if (peerId != null && !Arrays.equals(hs.getPeerId(), peerId)) {
                throw new ParseException("Announced peer ID " +
                                Torrent.byteArrayToHexString(hs.getPeerId()) +
                                " did not match expected peer ID " +
                                Torrent.byteArrayToHexString(peerId) + ".", pstrlen + 29);
        }

        return hs;
        
		}
	
	private void sendHandshake(Socket socket) throws IOException {
		System.out.println("in sendHandshake");
		Handshake hs = Handshake.make(this.torrent.getInfoHash(), 
				this.id.getBytes(Torrent.BYTE_ENCODING));
		OutputStream os = socket.getOutputStream();
        os.write(hs.getDataBytes(), 0, hs.getDataBytes().length);
	}
	
	public Peer connect(Peer peer) {
		socket = new Socket();
        InetSocketAddress address = peer.getInetSocketAddress();

        try {
                socket.connect(address, 3000);
        } catch (IOException ioe) {
                // Could not connect to peer, abort
        	System.out.println("could not connect to peer");
        	try { socket.close(); } catch (IOException e) { }
                //logger.warn("Could not connect to {}: {}", peer, ioe.getMessage());   
        	return null;
        }

        try {
                sendHandshake(socket);
                Handshake hs = getHandshake(socket,
                                (peer.hasPeerId() ? peer.getPeerId().array() : null));
                //this.fireNewPeerConnection(socket, hs.getPeerId());
                Peer ret = new Peer(address.getHostString(), address.getPort(), ByteBuffer.wrap(hs.getPeerId()));
                ret.setSocket(socket);
                return ret;
        } catch (ParseException pe) {
                //logger.debug("Invalid handshake from {}: {}",
                        //this.socketRepr(socket), pe.getMessage());
                try { socket.close(); } catch (IOException e) { }
        } catch (IOException ioe) {
                //logger.debug("An error occurred while reading an incoming " +
                               // "handshake: {}", ioe.getMessage());
                try {
                        if (!socket.isClosed()) {
                                socket.close();
                        }
                } catch (IOException e) {
                        // Ignore
                }
        }

        return null;
	}
}
