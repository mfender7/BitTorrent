import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.UUID;
import com.turn.ttorrent.common.Torrent;

/*import com.turn.ttorrent.client.ConnectionHandler;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.client.Client.ClientState;
import com.turn.ttorrent.client.announce.Announce;
import com.turn.ttorrent.client.peer.SharingPeer;
*/

public class Client {

	private Torrent torrent;
	private Peer peer;
	private boolean stop;
	private ConnectionListener service;
	private String id;
	
	public Client(InetAddress address, Torrent torrent)
		throws UnknownHostException, IOException {
		this.torrent = torrent;
		//this.state = ClientState.WAITING;

		id = "-MT0001-" + UUID.randomUUID()
				.toString().split("-")[4];

		// Initialize the incoming connection handler and register ourselves to
		// it.
		this.service = new ConnectionListener(this.torrent, id, address);
		//this.service.register(this);

		this.peer = new Peer(
			this.service.getAddress()
				.getAddress().getHostAddress(),
			(short)this.service.getAddress().getPort(),
			ByteBuffer.wrap(id.getBytes(Torrent.BYTE_ENCODING)));

		// Initialize the announce request thread, and register ourselves to it
		// as well.
		/*this.announce = new Announce(this.torrent, this.self);
		this.announce.register(this);

		logger.info("BitTorrent client [{}] for {} started and " +
			"listening at {}:{}...",
			new Object[] {
				this.self.getShortHexPeerId(),
				this.torrent.getName(),
				this.self.getIp(),
				this.self.getPort()
			});

		this.peers = new ConcurrentHashMap<String, SharingPeer>();
		this.connected = new ConcurrentHashMap<String, SharingPeer>();
		this.random = new Random(System.currentTimeMillis());*/
	}
	
	public void run(){
		this.service.start();
	}
	
	public String getID(){
		return this.id;
	}
	
	public ConnectionListener getService(){
		return this.service;
	}
}
