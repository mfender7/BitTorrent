import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
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
	}
	
	public Torrent getTorrent(){
		return this.torrent;
	}
	
	public int getPieces(){
		
		return pieces;
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
	
	public int getPiecesFromPeer(){
		//here get all pieces from peer that we can/need
		//for each piece that the peer has
		//if self(peer) doesn't have it
		//request it if unchoked
		System.out.println("need to implement torrentfile.getpiecesfrompeer?");
		return 0;
	}

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
					p.setPeer_choking(false);
					p.setAm_interested(true);
//					buffer = new PeerMessage().sendMessage(PeerMessage.Type.REQUEST.getType(), 0);
//					os.write(buffer.array());
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
				}
				else if (mes.getMessageID() == 0){
					System.out.println("Choked message! Abandon ship!");
					return null;
				}
				if(p.getAm_interested() && !p.getPeer_choking())
					return p;
			}
		} catch (Exception ex){
			System.out.println("Shit happened.");
		}
		return null;
	}
	
	public void requestPiece(){
		
	}
	
	
	public void downloadPiece(int piece, Peer p) throws IOException{
		//Start off at offset 0 for any piece.
		TorrentFilePart part = new TorrentFilePart(ByteBuffer.wrap(new byte[pieceLength]), 0);
		//Connect to the peer again since it worked the last time.
		Socket socket = new Socket();
		socket.connect(p.getInetSocketAddress(), 3000);
		//and nooooow we start pulling all the things...
		boolean finished = false;
		while(!finished){
			
		}
		
	}
	
	private static class TorrentFilePart{
		
		public static final int REQUEST_SIZE = 16384;
		
		private ByteBuffer piece;
		private long offset;
		private byte[] data;
		
		public TorrentFilePart(ByteBuffer piece, long offset){
			this.piece = piece;
			this.offset = offset;
		}
	}
	
	private static class PeerMessage{
		
		//CONSTANTS, YEAH
		public enum Type {
			KEEP_ALIVE(-1),
			CHOKE(0),
			UNCHOKE(1),
			INTERESTED(2),
			NOT_INTERESTED(3),
			HAVE(4),
			BITFIELD(5),
			REQUEST(6),
			PIECE(7),
			CANCEL(8);

			private int id;
			Type(int id) {
				this.id = id;
			}

			public boolean equals(int c) {
				return this.id == c;
			}

			public int getType() {
				return this.id;
			}

			public static Type get(int c) {
				for (Type t : Type.values()) {
					if (t.equals(c)) {
						return t;
					}
				}
				return null;
			}
		};
		
		//actual fields
		private int length;
		private int messageID;
		private byte[] payload;
		private int index;
		private int blockOffset;
		private int blockOther;
		private Torrent torrent;
		private int piece;
		
		
		public PeerMessage(){ }
		
		public static ByteBuffer parseHeader(InputStream stream) throws IOException {
			byte[] length = new byte[4];
			int r = stream.read(length, 0, length.length);  //something hanging here when calling from while loop
			System.out.println("first # of bytes read (header) " + r);
			ByteBuffer buffer = ByteBuffer.wrap(length);

			/*//int len = ByteBuffer.wrap(length).getInt();
			int len = buffer.getInt();
			System.out.println("len = "+len);
			byte[] body = new byte[len];
			stream.read(body);
			buffer = ByteBuffer.allocate(4 + len);
			buffer.put(length).put(body);*/
			
			int len = buffer.getInt();
			if(len == 0 && r != -1) {//wat is this
				System.out.println("calling parseHeader w/in parseHeader");
				return parseHeader(stream);
			}
			//int mi = buffer.get();
			byte[] body = new byte[len];
			r = stream.read(body);
			System.out.println("# of bytes read " + r);
			buffer = ByteBuffer.allocate(4 + len);
			buffer.put(length);
			buffer.put(body);
			return buffer;
		}
		
		public PeerMessage(ByteBuffer message, TorrentFile file, Peer self, Socket s){
			//byte[] field = new byte[4]; 
			message.rewind();
			this.length = message.getInt(); //payload length
			//field = new byte[1];
			messageID = message.get();  //messageID
			System.out.println("messageID = "+messageID);
			//payload = new byte[length]; //get that payload ready
			this.payload = new byte[length - 1];
			message.get(payload); //put the payload stuff into its own array, dang it
			if(messageID >= -1 && messageID <= 8) {
				parse(message, file, self, s);
			}
		}
		
		//parse the messages that actually have a payload
		private void parse(ByteBuffer message, TorrentFile file, Peer self, Socket s){
			Type type = Type.get((byte) messageID);
			System.out.println(type);
			ByteBuffer messageBuffer;
			switch(type){
				case KEEP_ALIVE:
					break;
				case CHOKE:
					self.setPeer_choking(true);
					break;
				case UNCHOKE:
					self.setPeer_choking(false);
					break;
				case INTERESTED:
					self.setPeer_interested(true);
					break;
				case NOT_INTERESTED:
					self.setPeer_interested(false);
					break;
				case HAVE:
					System.out.println("THEY HAS SOMETHING");
					ByteBuffer payloadBuff = ByteBuffer.wrap(payload);
					int piece = payloadBuff.getInt();
					OutputStream os;
					InputStream is;
					//validate index
					if (piece >= 0 && piece < file.getPieces()){
						if (!self.findTorrentPiece(file.torrent, piece)){
							//send unchoke message
							messageBuffer = new PeerMessage().sendMessage(PeerMessage.Type.UNCHOKE.getType(), 0);							
							/*try{
								os = s.getOutputStream();
								os.write(messageBuffer.array());
								System.out.println("unchoked message sent.");
								self.setAm_choking(false);
								is = s.getInputStream();
								messageBuffer = PeerMessage.parseHeader(is);
								int c = messageBuffer.array()[4];
								if(c == 1){ System.out.println("Peer is unchoked"); self.setPeer_choking(false);}
							} catch (Exception ex){
								
							}
							
							
							//send interested message
							messageBuffer = new PeerMessage().sendMessage(PeerMessage.Type.INTERESTED.getType(), 0);
							//os.write(buffer.array(), 0, buffer.array().length);
							
							try {
								os = s.getOutputStream();
								os.write(messageBuffer.array());
								System.out.println("Interest message sent");
								self.setAm_interested(true);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							//confirm peer isn't choked
							
							if (!self.getPeer_choking()){ //not being choked by peer, request piece
								//send a request message
								messageBuffer.clear();
								messageBuffer = new PeerMessage().sendMessage(PeerMessage.Type.REQUEST.getType(), piece);
								try {
									os = s.getOutputStream();
									os.write(messageBuffer.array());
									System.out.println("Request message sent");
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}*/
						}
					}
					break;
				case BITFIELD:
					message.position(5);
					System.out.println(new String(Hex.encodeHex(payload)));
					System.out.println(new String(Hex.encodeHex(payload)).length());
					BitSet bitfield = new BitSet(payload.length);
					List<Integer> indices = new ArrayList<Integer>();

					//byte[] onTheLeft = new byte[]{1, 0, 0, 0, 0, 0, 0, 0};
					//BitSet bs = BitSet.valueOf(onTheLeft);
					for(int i = 0; i < payload.length; i++){
						byte a = payload[i];
						for(int j = 0; j < 8; j++){
							int index = i * 8 + j;
							if ((a & (0x10000000 >> j)) != 0){
								indices.add(index);
								//System.out.println("Added index: " + index);
							}
						}
					}
					file.getCurrentPeer().addDownloadedTorrentPiecesList(file.getTorrent(), indices);

					System.out.println("Bitfield bullshit");
					
					//... pull out the list of 
					
					break;
				case REQUEST:
					break;
				case PIECE:
					byte[] field = new byte[4];
					message.get(field); //piece index!
					if(index == ByteBuffer.wrap(field).getInt())
						System.out.println("Alright, go go go.");
					field = new byte[4];
					message.get(field);
					if(blockOffset == ByteBuffer.wrap(field).getInt())
						System.out.println("Yay, right offset!");
					payload = new byte[length - 9];
					message.get(payload);
					blockOffset += 1;
					
					break;
				case CANCEL:
					break;
			}
		}
		
		public int getMessageID(){
			return messageID;
		}
		
		public int getLength(){
			return length;
		}
		
		public byte[] getPayload(){
			return payload;
		}
		
		public ByteBuffer sendMessage(int id, int piece){
			Type type = Type.get(id);
			switch(type){
				case CHOKE:
					return sendChoke();
				case UNCHOKE:
					return sendUnchoke();
				case INTERESTED:
					return sendInterested();
				case NOT_INTERESTED:
					return sendNotInterested();
				case HAVE:
					//payload is a number denoting the index of a piece 
					//that the peer has successfully downloaded and validated
					return sendHave();
				case BITFIELD:
					return sendBitfield();
				case REQUEST:
					return sendRequest(piece);
				case PIECE:
					return sendPiece();
				case CANCEL:
					return sendCancel();
				default:
					return null;
			}
		}
		
		public ByteBuffer craft(){
			ByteBuffer buffer = ByteBuffer.allocate(4 + length);
			buffer.rewind();
			buffer.putInt(length);
			buffer.put((byte)messageID);
			if(length > 1){				
				buffer.put(payload);
			}
			//buffer.rewind();
			return buffer;
		}
		
		private ByteBuffer sendChoke(){
			length = 1;
			messageID = 0;
			return craft();
		}
		
		private ByteBuffer sendUnchoke(){
			length = 1;
			messageID = 1;
			return craft();
		}
		
		private ByteBuffer sendInterested(){
			length = 1;
			messageID = 2;
			return craft();
		}
		
		private ByteBuffer sendNotInterested(){
			length = 1;
			messageID = 3;
			return craft();
		}
		
		private ByteBuffer sendHave(){
			length = 0;
			messageID = 4;
			return craft();
		}
		
		private ByteBuffer sendBitfield(){
			length = 0;
			messageID = 5;
			return craft();
		}
		
		private ByteBuffer sendRequest(int piece){
			length = 12; //payload length 12
			messageID = 6;
			this.piece = piece;
			return craft();
		}
		
		private ByteBuffer sendPiece(){
			length = 0;
			messageID = 7;
			return craft();
		}
		
		private ByteBuffer sendCancel(){
			length = 0;
			messageID = 8;
			return craft();
		}

	}
	
}
