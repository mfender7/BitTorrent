import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
	private Peer peer;
	
	public TorrentFile(Torrent torrent) throws IOException {
		this.torrent = torrent;
		this.pieceLength = torrent.getDecodedInfo().get("piece length").getInt();
		//torrent.
		this.raf = new RandomAccessFile(new File(torrent.getName()), "rw");
		this.channel = raf.getChannel();
		this.fileSize = torrent.getSize();
		this.offset = 0L;
		this.pieces = (int)Math.ceil((double)this.fileSize / this.pieceLength) + 1;
		this.peer = peer;
	}
	
	public int getPieces(){
		return pieces;
	}

	public Socket establishPeer(Peer p){
		try{
			Socket socket = new Socket();
			socket.connect(p.getInetSocketAddress(), 3000);
			OutputStream os = socket.getOutputStream();
			//write/send stuff
			ByteBuffer buffer = new PeerMessage().sendMessage(PeerMessage.Type.UNCHOKE.getType());
			System.out.println("here1");
			os.write(buffer.array(), 0, buffer.array().length);
			System.out.println("here2");
			InputStream is = socket.getInputStream();
			byte[] resp = new byte[5];
			int read = is.read(resp);
			System.out.println("here3");
			buffer = new PeerMessage().sendMessage(PeerMessage.Type.INTERESTED.getType());
			System.out.println("here4");
			os.write(buffer.array(), 0, buffer.array().length);
			read = is.read();
			System.out.println("Read " + read);
			//Wait for bitfield or have mesage, then 
			System.out.println("I have no idea how to figure out whether we're good to go or not.....");
			//socket.close();
			return socket;
		} catch (Exception ex){
			System.out.println("Shit, what happened.");
			System.out.println(ex.getMessage());
			
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
		
		
		
		public PeerMessage(){ }
		
		public PeerMessage(ByteBuffer message){
			byte[] field = new byte[1]; 
			length = message.getInt(); //payload length
			field = new byte[1];
			messageID = message.getInt();  //messageID
			//payload = new byte[length]; //get that payload ready
			field = new byte[length - 1];
			message.get(field); //put the payload stuff into its own array, dang it
			if(messageID >= Type.HAVE.getType())
				parse(message);
		}
		
		//parse the messages that actually have a payload
		private void parse(ByteBuffer message){
			Type type = Type.get((byte) messageID);
			
			switch(type){
				case HAVE:
					//payload is a number denoting the index of a piece 
					//that the peer has successfully downloaded and validated
				case BITFIELD:
					
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
			buffer.putInt(1);
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
