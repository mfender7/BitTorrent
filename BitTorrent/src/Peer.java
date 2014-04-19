import com.turn.ttorrent.common.Torrent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class Peer {

	private InetSocketAddress address;
	private String hostId;

	private ByteBuffer peerId; // ?
	private String hexPeerId;
	private String ip;
	private int port;
	private boolean interested;
	private boolean choked;


	public Peer(String ip, int port, ByteBuffer peerId) {
		this.address = new InetSocketAddress(ip, port);
		this.hostId = String.format("%s:%d",
				this.address.getAddress(),
				this.address.getPort());  // ?
		this.port = port;
		
		this.peerId = peerId;
		this.hexPeerId = Torrent.byteArrayToHexString(peerId.array()); // ?
	}


	public Peer(InetSocketAddress inetSocketAddress) {
		this.address = inetSocketAddress;
		this.hostId = String.format("%s:%d",
				this.address.getAddress(),
				this.address.getPort());  // ?
	}
	
	public InetSocketAddress getInetSocketAddress(){
		return this.address;
	}
	
	public boolean hasPeerId() {
		return this.peerId != null;
	}

	public ByteBuffer getPeerId() {
		return this.peerId;
	}
	
	/*public String toString() {
		StringBuilder s = new StringBuilder("peer://")
			.append(this.getIp()).append(":").append(this.getPort())
			.append("/");

		if (this.hasPeerId()) {
			s.append(this.hexPeerId.substring(this.hexPeerId.length()-6));
		} else {
			s.append("?");
		}

		if (this.getPort() < 10000) {
			s.append(" ");
		}

		return s.toString();
	}*/

}
