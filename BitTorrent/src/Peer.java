import com.turn.ttorrent.common.Torrent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class Peer {

	private InetSocketAddress address;
	private String hostId;

	private ByteBuffer peerId; // ?
	private String hexPeerId;


	public Peer(String ip, int port, ByteBuffer peerId) {
		this.address = new InetSocketAddress(ip, port);
		this.hostId = String.format("%s:%d",
				this.address.getAddress(),
				this.address.getPort());  // ?
		
		this.peerId = peerId;
		this.hexPeerId = Torrent.byteArrayToHexString(peerId.array()); // ?
	}


}
