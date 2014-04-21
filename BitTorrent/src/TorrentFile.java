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
	private Peer peer;
	private Peer self;
	
	public TorrentFile(Torrent torrent, Client client) throws IOException {
		this.torrent = torrent;
		this.pieceLength = torrent.getDecodedInfo().get("piece length").getInt();
		//torrent.
		//this.raf = new RandomAccessFile(new File(torrent.getName()), "rw");
		//this.channel = raf.getChannel();
		this.fileSize = torrent.getSize();
		this.offset = 0L;
		this.pieces = (int)Math.ceil((double)this.fileSize / this.pieceLength) + 1;
		this.peer = peer;
		this.self = client.getPeer();
	}
	
	public int getPieces(){
		
		return pieces;
	}
	
	public int getPiecesFromPeer(){
		//here get all pieces from peer that we can/need
		System.out.println("need to implement torrentfile.getpiecesfrompeer?");
		return 0;
	}

	public Socket establishPeer(Peer p){
		try{ 
			Socket socket = p.getSocket();
			InputStream is = socket.getInputStream();
			//first let's get dat bitfield...
			ByteBuffer buffer = PeerMessage.parseHeader(is);
			System.out.println("wat");
			PeerMessage mes = new PeerMessage(buffer, this, this.self, socket);
			if(mes.getMessageID() == 5){
				//Bitfield, so we need to read in the next HAVE message
				System.out.println("BITFIELD, READING IMMINENT HAVE");	
				buffer = PeerMessage.parseHeader(is);
				mes = new PeerMessage(buffer, this, this.self, socket);
			}
			else{
				System.out.println("JUST HAVE");
			}
			
			buffer = ByteBuffer.allocate(0);
			//unchoked message
			OutputStream os = socket.getOutputStream();
			//write/send stuff
			buffer.clear();
			buffer = new PeerMessage().sendMessage(PeerMessage.Type.UNCHOKE.getType());
			os.write(buffer.array());
			System.out.println(socket.isClosed());
			is = socket.getInputStream();
			//System.out.println(is.read());
			buffer.clear();
			buffer = PeerMessage.parseHeader(is);
			PeerMessage mess = new PeerMessage(buffer, this, this.self, socket);
			//interested message
//			buffer = new PeerMessage().sendMessage(PeerMessage.Type.INTERESTED.getType());
//			os.write(buffer.array(), 0, buffer.array().length);
//			buffer = PeerMessage.parseHeader(is);

			return socket;
		} catch (Exception ex){
			System.out.println("Shit happened.");
			//ex.printStackTrace();
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
		
		
		public PeerMessage(){ }
		
		public static ByteBuffer parseHeader(InputStream stream) throws IOException {
			byte[] length = new byte[5];
			int r = stream.read(length, 0, length.length);
			System.out.println("read " + r);
			ByteBuffer buffer = ByteBuffer.wrap(length);

			/*//int len = ByteBuffer.wrap(length).getInt();
			int len = buffer.getInt();
			System.out.println("len = "+len);
			byte[] body = new byte[len];
			stream.read(body);
			buffer = ByteBuffer.allocate(4 + len);
			buffer.put(length).put(body);*/
			
			int len = buffer.getInt();
			if(len == 0 && r != -1)
				return parseHeader(stream);
			int mi = buffer.get();
			byte[] body = new byte[1];
			if(len > 0){
				body = new byte[len - 1];
				r = stream.read(body, 0, len - 1);
				System.out.println("read " + r);
				buffer = ByteBuffer.allocate(4 + len);
				buffer.put(length);
				buffer.put(body);
			}

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
			if(messageID >= Type.HAVE.getType()) {
				parse(message, file, self, s);
			}
		}
		
		//parse the messages that actually have a payload
		private void parse(ByteBuffer message, TorrentFile file, Peer self, Socket s){
			Type type = Type.get((byte) messageID);
			System.out.println(type);
			ByteBuffer messageBuffer;
			switch(type){
				case HAVE:
					System.out.println("THEY HAS SOMETHING");
					ByteBuffer payloadBuff = ByteBuffer.wrap(payload);
					int piece = payloadBuff.getInt();
					//validate index
					if (piece >= 0 && piece < file.getPieces()){
						if (!self.findTorrentPiece(file.torrent, piece)){
							//send interested message
							messageBuffer = new PeerMessage().sendMessage(PeerMessage.Type.INTERESTED.getType());
							//os.write(buffer.array(), 0, buffer.array().length);
							OutputStream os;
							try {
								os = s.getOutputStream();
								os.write(messageBuffer.array());
								System.out.println("Interest message sent");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							//maybe send a request message
						}
					}
					break;
				case BITFIELD:
					message.position(5);
					System.out.println(new String(Hex.encodeHex(payload)));
					System.out.println(new String(Hex.encodeHex(payload)).length());
					BitSet bitfield = new BitSet(payload.length);
					List<Integer> indeces = new ArrayList<Integer>();
					for(int i = 0; i < message.remaining()*8; i++){
						if ((message.get(i/8) & (1 << (7 -(i % 8)))) > 0){
							//indeces.add(i);
							
							bitfield.set(i);
						}
					}
					
					//byte[] onTheLeft = new byte[]{1, 0, 0, 0, 0, 0, 0, 0};
					//BitSet bs = BitSet.valueOf(onTheLeft);
					for(int i = 0; i < payload.length; i++){
						byte a = payload[i];
						for(int j = 0; j < 8; j++){
							int index = i * 8 + j;
							if ((a & (0x10000000 >> j)) != 0){
								indeces.add(index);
								System.out.println("Added index: " + index);
							}
						}
					}

					System.out.println("Bitfield bullshit");
					
					//... pull out the list of 
					
					break;
				case REQUEST:
					
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
		
		public ByteBuffer sendMessage(int id){
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
					return sendRequest();
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
		
		private ByteBuffer sendPiece(){
			length = 0;
			messageID = 5;
			return craft();
		}
		
		private ByteBuffer sendCancel(){
			length = 0;
			messageID = 6;
			return craft();
		}
		
		private ByteBuffer sendRequest(){
			length = 0;
			messageID = 7;
			return craft();
		}
		
		private ByteBuffer sendBitfield(){
			length = 0;
			messageID = 4;
			return craft();
		}
	}
	
}
