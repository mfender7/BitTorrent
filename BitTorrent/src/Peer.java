import com.turn.ttorrent.common.Torrent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Peer {

	private InetSocketAddress address;
	private String hostId;

	private ByteBuffer peerId; // ?
	private String hexPeerId;
	private String ip;
	private int port;
	private Socket socket;
	private Map<Torrent, List<Integer>> downloadedTorrentPieces;
	
	/*am_choking: this client is choking the peer
am_interested: this client is interested in the peer
peer_choking: peer is choking this client
peer_interested: peer is interested in this client*/
	private boolean am_choking;
	private boolean am_interested;
	private boolean peer_choking;
	private boolean peer_interested;


	public Peer(String ip, int port, ByteBuffer peerId) {
		this.address = new InetSocketAddress(ip, port);
		this.hostId = String.format("%s:%d",
				this.address.getAddress(),
				this.address.getPort());  // ?
		this.port = port;
		this.downloadedTorrentPieces = new HashMap();
		
		this.peerId = peerId;
		this.hexPeerId = Torrent.byteArrayToHexString(peerId.array()); // ?
		
		am_choking = true;
		am_interested = false;
		peer_choking = true;
		peer_interested = false;
	}


	public Peer(InetSocketAddress inetSocketAddress) {
		this.address = inetSocketAddress;
		this.hostId = String.format("%s:%d",
				this.address.getAddress(),
				this.address.getPort());  // ?
	}
	
	public void setAm_choking(boolean b){
		this.am_choking=b;
	}
	public void setAm_interested(boolean b){
		this.am_interested=b;
	}
	public void setPeer_choking(boolean b){
		this.peer_choking=b;
	}
	public void setPeer_interested(boolean b){
		this.peer_interested=b;
	}
	public boolean getAm_choking(){
		return this.am_choking;
	}
	public boolean getAm_interested(){
		return this.am_interested;
	}
	public boolean getPeer_choking(){
		return this.peer_choking;
	}
	public boolean getPeer_interested(){
		return this.peer_interested;
	}

	public void addDownloadedTorrentPiece(Torrent torrent, int piece){
		if (downloadedTorrentPieces.containsKey(torrent)){
			List<Integer> list = downloadedTorrentPieces.get(torrent);
			list.add(piece);
		}
		else { //torrent not in the list yet
			List<Integer> newList = new ArrayList<Integer>();
			newList.add(piece);
			downloadedTorrentPieces.put(torrent, newList);
		}
	}
	
	public boolean findTorrentPiece(Torrent torrent, int piece){
		if (downloadedTorrentPieces.containsKey(torrent)){
			List<Integer> list = downloadedTorrentPieces.get(torrent);
			if (list.contains(piece)){
				return true;
			}
		}
		return false;
	}
	
	public void addDownloadedTorrentPiecesList(Torrent torrent, List<Integer> pieceList){
		if (downloadedTorrentPieces.containsKey(torrent)){
			List<Integer> list = downloadedTorrentPieces.get(torrent);
			for (int i : pieceList){
				if (!list.contains(i)){
					list.add(i);
				}
			}
		}
		else { //torrent not in the list yet
			//List<Integer> newList = new ArrayList<Integer>();
			downloadedTorrentPieces.put(torrent, pieceList);
		}
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
	
	public Socket getSocket(){
		return socket;
	}
	
	public void setSocket(Socket socket){
		this.socket = socket;
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
