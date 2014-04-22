import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

import com.turn.ttorrent.common.Torrent;

public class TorrentFile {
	
	private long fileSize;
	private long offset;
	private int pieces;
	private int pieceLength;
	private RandomAccessFile raf;
	private FileChannel channel;
	private Torrent torrent;
	private File torrentFile;
	private Map<Integer, TorrentFilePart> torrentParts;
	private List<Peer> peerList;
	private Peer self;
	private Peer currentPeer;
	private List<Integer> indices;
	
	public TorrentFile(Torrent torrent, Client client) throws IOException {
		this.torrent = torrent;
		this.pieceLength = torrent.getDecodedInfo().get("piece length").getInt();
		//torrent.
		//this.raf = new RandomAccessFile(new File(torrent.getName()), "rw");
		//this.channel = raf.getChannel();
		this.fileSize = torrent.getSize();
		this.offset = 0L;
		this.pieces = (int)Math.ceil((double)this.fileSize / this.pieceLength) + 1;
		this.self = client.getPeer();
		this.peerList = new ArrayList<Peer>();
		this.torrentParts = new HashMap<Integer, TorrentFilePart>();
		this.indices = new ArrayList<Integer>();
	}
	
	public Torrent getTorrent(){
		return this.torrent;
	}
	
	public int getPieces(){
		
		return pieces;
	}
	
	public void addIndexToIndices(int i){
		this.indices.add(i);
	}
	
	public List<Peer> getPeerList(){
		return this.peerList;
	}
	
	public void addToPeerList(Peer p){
		if (!this.peerList.contains(p)){
			this.peerList.add(p);
		}
	}
	
	public Peer getCurrentPeer(){
		return this.currentPeer;
	}
	
	public void setCurrentPeer(Peer p){
		this.currentPeer = p;
	}
	
	public void addTorrentPart(int piece, TorrentFilePart part){
		if(torrentParts.containsKey(piece))
			torrentParts.put(piece, part);
	}
	
	public int getPiecesFromPeer() throws IOException{
		//here get all pieces from peer that we can/need
		//for each piece that the peer has
		//if self(peer) doesn't have it
		//request it if unchoked
		Map<Torrent, List<Integer>> currentPeerPieces = currentPeer.getDownloadedTorrentPiecesList();
		//Map<Torrent, List<Integer>> selfPieces = self.getDownloadedTorrentPiecesList();
		Socket socket = this.currentPeer.getSocket();
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();
		List<Integer> list = currentPeerPieces.get(this.torrent);
		for (int i : list){
			if (!torrentParts.containsKey(i)){
				//REQUEST IT
				int offset = 0;
				TorrentFilePart part = new TorrentFilePart(this, i, offset, pieceLength);
				while(offset < pieceLength){
					//assume we're unchoked
					//overload sendmessge so we can handle REQUEST messages and their offsets. TODO FOR THE SILLY ONE.
					ByteBuffer buffer = new PeerMessage().sendMessage(PeerMessage.Type.REQUEST.getType(), i, offset);
					os.write(buffer.array());
					System.out.println("request sent for piece index "+i);
					
					//problem here TODO
					//we get choked: how to move on?
					//we get a HAVE message: causes concurrentmodificationexception
					buffer = PeerMessage.parseHeader(is);
					int mid = buffer.get(4);
					PeerMessage pm = new PeerMessage(buffer, this, this.currentPeer, socket);
					//split out payload, add to map
					part.add(pm.getPayload(), offset);
					//need to loop through every offset in the piece.
					//add piece to list of downloaded.
					//rinse and repeat TODO
				
					offset += PeerMessage.REQUEST_SIZE;
				}
				System.out.println("I think we're finally done...");
			}
		}
		//Here add into the downloadedTorrentPieces the indices received from peer while going through
		//the for loop
		//System.out.println("need to implement torrentfile.getpiecesfrompeer?");
		return 0;
	}

	
//	1) send handshake, 2) receive handshake 3) receive bitfield 4) send unchoke 
//	5) receive unchoke 6) send interested and request for some pieces
	
	/*in here we need to add the established peer to the list and set it as the currentpeer
	 * */
	public Peer establishPeer(Peer p){
		try{ 
			this.addToPeerList(p); //add it here?
			this.currentPeer = p;
			Socket socket = p.getSocket();
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			//first let's get dat bitfield...
			ByteBuffer buffer; // = PeerMessage.parseHeader(is);
			PeerMessage mes;
			while(true){
				buffer = PeerMessage.parseHeader(is);
				mes = new PeerMessage(buffer, this, this.self, socket);
				if(mes.getMessageID() == 5){
					//Bitfield, so we need to read in the next HAVE message
					System.out.println("Got a Bitfield message!");
				}
				else if (mes.getMessageID() == 4){
					System.out.println("JUST HAVE");
					//let's send an interested? maybe? possibly? or unchoked?
					///wait, that probably means they're interested *and* not choked. Su-weeet
					//p.setPeer_choking(false);
					p.setAm_interested(true);
					//buffer = new PeerMessage().sendMessage(PeerMessage.Type.UNCHOKE.getType(), 0);
					//os.write(buffer.array());
				}
				else if(mes.getMessageID() == 3){
					System.out.println("That meanie face peer isn't interested in us. Hmph. We'll go find another peer.");
					return null;
				}
				else if(mes.getMessageID() == 2){
					p.setPeer_interested(true);
					System.out.println("Are... are you interested? Really??? :D");
				}
				else if(mes.getMessageID() == 1){
					p.setPeer_choking(false);
					System.out.println("Unchoked!");
					buffer = new PeerMessage().sendMessage(PeerMessage.Type.INTERESTED.getType(), 0);
					os.write(buffer.array());
					p.setAm_interested(true);
				}
				else if (mes.getMessageID() == 0){
					System.out.println("Choked message! Abandon ship!");
					return null;
				}
				if(p.getAm_interested() && !p.getPeer_choking())
					return peer;
			}
		} catch (Exception ex){
			System.out.println("Shit happened.");
			ex.printStackTrace();
		}
		return null;
	}
}
