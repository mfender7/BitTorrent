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
	private int recentlyAnnouncedIndex;
	public boolean inGetPiecesLoop;

	public TorrentFile(Torrent torrent, Client client) throws IOException {
		this.torrent = torrent;
		this.pieceLength = torrent.getDecodedInfo().get("piece length")
				.getInt();
		// torrent.
		// this.raf = new RandomAccessFile(new File(torrent.getName()), "rw");
		// this.channel = raf.getChannel();
		this.fileSize = torrent.getSize();
		this.offset = 0L;
		this.pieces = (int) Math
				.ceil((double) this.fileSize / this.pieceLength) + 1;
		this.self = client.getPeer();
		this.peerList = new ArrayList<Peer>();
		this.torrentParts = new HashMap<Integer, TorrentFilePart>();
		this.indices = new ArrayList<Integer>();
		this.recentlyAnnouncedIndex = -1;
		this.inGetPiecesLoop = false;
	}
	
	
	public int getRecentlyAnnouncedIndex(){
		return this.recentlyAnnouncedIndex;
	}
	
	public void setRecentlyAnnouncedIndex(int i){
		this.recentlyAnnouncedIndex = i;
	}

	public Torrent getTorrent() {
		return this.torrent;
	}

	public boolean weAreDone() {
		for (int i = 0; i < pieces; i++)
			if (!torrentParts.containsKey(i)
					|| (torrentParts.containsKey(i) && !torrentParts.get(i)
							.isComplete()))
				return false;
		return true;
	}

	public int getPieces() {

		return pieces;
	}

	public void addIndexToIndices(int i) {
		this.indices.add(i);
	}

	public List<Peer> getPeerList() {
		return this.peerList;
	}

	public void addToPeerList(Peer p) {
		if (!this.peerList.contains(p)) {
			this.peerList.add(p);
		}
	}

	public Peer getCurrentPeer() {
		return this.currentPeer;
	}

	public void setCurrentPeer(Peer p) {
		this.currentPeer = p;
	}

	public void addTorrentPart(int piece, TorrentFilePart part) {
		if (torrentParts.containsKey(piece))
			torrentParts.put(piece, part);
	}

	//the abysmal method that takes away hours of your time
	//JUST TO MAKE SURE ALL THE FUCKING OFFSET PIECES DONT' CRASH
	//;LFKJDASL;FJKADLS;FKJADLS;FJADLS;
	public int getPiecesFromPeer() throws IOException {
		Map<Torrent, List<Integer>> currentPeerPieces = currentPeer.getDownloadedTorrentPiecesList();
		Socket socket = this.currentPeer.getSocket();
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();
		List<Integer> list = currentPeerPieces.get(this.torrent);
		int HAVEcounter = 0;
		for (int i : list) {
			this.inGetPiecesLoop = true;
			if (!torrentParts.containsKey(i) || (torrentParts.containsKey(i) && !torrentParts.get(i).isComplete())) {
				// REQUEST IT
				int offset = 0;
				TorrentFilePart part;
				ByteBuffer buffer;
				//our map of parts that we keep track of at all times. 
				if (!torrentParts.containsKey(i)) { //if we haven't started on it yet, create and add to map
					part = new TorrentFilePart(this, i, offset, pieceLength);
					torrentParts.put(i, part);
				} else { //Ahah! It exists! We'll just start off where it last ended.
					part = torrentParts.get(i);
					offset = part.getOffset();
				}
				while (offset < pieceLength) {
					if (i==66){
						System.out.println("sldkjfl");
					}
					buffer = new PeerMessage().sendMessage(PeerMessage.Type.REQUEST.getType(), i, offset);
					os.write(buffer.array());
					System.out.println("request sent for piece index " + i + " at offset " + offset);
					buffer = PeerMessage.parseHeader(is);
					if (buffer.capacity() > 0) { //I don't like reading negative bits....
						/*if (buffer.getInt()==0){
							//KEEP ALIVE
							System.out.println("keep alive detected");
							continue;
						}*/
						/*if (buffer.remaining()<5){
							continue;
						}*/
						int mid = buffer.get(4); //but then this will kill it if we don't have 5 bytes. but I also don't want it looping forever..
						if (mid == 4){
							//got a damned have, request the piece it's telling us about
							/*PeerMessage pm = new PeerMessage(buffer, this, this.currentPeer, socket);
							int derp = this.getRecentlyAnnouncedIndex();
							if (derp!=-1){
								buffer = new PeerMessage().sendMessage(PeerMessage.Type.REQUEST.getType(), derp, offset);
								os.write(buffer.array());
								System.out.println("request sent for piece index " + derp + " at offset " + offset);
							}*/
							HAVEcounter++;
							if (HAVEcounter > 4){
								break;
							}
							continue;
						}
						else if (mid == 7) {
							PeerMessage pm = new PeerMessage(buffer, this, this.currentPeer, socket); //parse out the payload
							part.add(pm.getPayload(), offset); //add the piece[offset] to our TorrentFilePart map
							offset += PeerMessage.REQUEST_SIZE;
						}
						else {
							System.out.println("wtf");
						}
					}
					//also, fuck skipping around offsets. we're brute-forcing each piece, whether we like it or not.
				}
				if (HAVEcounter > 4){
					break;
				}
				part.setComplete(true);
				System.out.println("I think we're finally done...");
			}
		}
		this.inGetPiecesLoop = false;
		if (this.indices.size() > 0){
			for (int i : this.indices){
				this.getCurrentPeer().addDownloadedTorrentPiece(this.torrent, i);
			}
			getPiecesFromPeer(); //see if there are new pieces to add
		}

		if (weAreDone()) {
			return 1;
		}
		return 0;
	}

	/*
	 * in here we need to add the established peer to the list and set it as the
	 * currentpeer
	 */
	public Peer establishPeer(Peer p) {
		try {
			this.addToPeerList(p); // add it here?
			this.currentPeer = p;
			Socket socket = p.getSocket();
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			// first let's get dat bitfield...
			ByteBuffer buffer; // = PeerMessage.parseHeader(is);
			PeerMessage mes;
			while (true) {
				buffer = PeerMessage.parseHeader(is);
				mes = new PeerMessage(buffer, this, this.self, socket);
				if (mes.getMessageID() == 5) {
					// Bitfield, so we need to read in the next HAVE message
					System.out.println("Got a Bitfield message!");
					//buffer = new PeerMessage().sendMessage(PeerMessage.Type.BITFIELD.getType(), 0);
					//os.write(buffer.array());					
				} else if (mes.getMessageID() == 4) {
					System.out.println("JUST HAVE");
					p.setPeer_choking(false);
					// I don't know why, but no setting it as unchoking made it look for more messages,
					//which then results in an unchoke.
					p.setAm_interested(true);
				} else if (mes.getMessageID() == 3) {
					System.out.println("That meanie face peer isn't interested in us. Hmph. We'll go find another peer.");
					return null;
				} else if (mes.getMessageID() == 2) {
					p.setPeer_interested(true);
					System.out.println("Are... are you interested? Really??? :D");
				} else if (mes.getMessageID() == 1) {
					//the true culprit on how to guarantee we get requests/pieces. 
					//I have no fucking idea why, but as soon as we get an unchoked
					//the next message we get later will be a request. or, we'll actually
					//receive a piece message when we send a request after receiving an unchoke
					p.setPeer_choking(false);
					System.out.println("Unchoked!");
					buffer = new PeerMessage().sendMessage(PeerMessage.Type.INTERESTED.getType(), 0);
					os.write(buffer.array());
					p.setAm_interested(true);
				} else if (mes.getMessageID() == 0) {
					System.out.println("Choked message! Abandon ship!");
					return null;
				}
				if (p.getAm_interested() && !p.getPeer_choking())
					return p;
			}
		} catch (Exception ex) {
			System.out.println("Shit happened.");
			ex.printStackTrace();
		}
		return null;
	}
}
