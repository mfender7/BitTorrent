import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class PeerMessage {

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
	private int piece;
	private TorrentFile file;
	public static final int REQUEST_SIZE = 16384;
	
	public PeerMessage(){ }
	
	public static ByteBuffer parseHeader(InputStream stream) throws IOException {
		byte[] length = new byte[4];
		int r = stream.read(length, 0, length.length);
		while ((r>-1) && (r<4)){
			r+= stream.read(length, r, 4-r);
			//System.out.println("r < 4, now r = "+r);
		}
		ByteBuffer buffer = ByteBuffer.wrap(length);
		int len = buffer.getInt();
		//System.out.println("first # of bytes read (header) " + r + " expected length: " + len);
		if (r == 0) {
			return buffer;
		}
		byte[] body = new byte[len];
		r = stream.read(body);
		while ((r>-1) && (r<len)){
			r+= stream.read(body, r, len-r);
			//System.out.println("# of bytes read " + r);
		}
		//System.out.println("# of bytes read " + r);
		buffer = ByteBuffer.allocate(4 + len);
		buffer.put(length);
		buffer.put(body);
		return buffer; //if we get r is -1, this will return a buffer of [0,0,0,0]
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
		//System.out.println(type);
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
				//System.out.println("THEY HAS SOMETHING");
				ByteBuffer payloadBuff = ByteBuffer.wrap(payload);
				int piece = payloadBuff.getInt();
				//validate index
				if (piece >= 0 && piece < file.getPieces()){
					/*if not in currentPeer's list, add it*/
					if (!file.getCurrentPeer().findTorrentPiece(file.getTorrent(), piece)){
						if (!file.inGetPiecesLoop) { 
							file.getCurrentPeer().addDownloadedTorrentPiece(file.getTorrent(), piece);
						}
						else {
							//YEAH, FUCK YOU TOO
							file.addIndexToIndices(piece);
						}
						
					}
					file.setRecentlyAnnouncedIndex(piece);
					
				}
				break;
			case BITFIELD:
				message.position(5);
				List<Integer> indices = new ArrayList<Integer>();
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
				
				break;
			case REQUEST:
				break;
			case PIECE:
				System.out.println("I GOT A PIECE");
				message.rewind();
				this.length = message.getInt();
				this.messageID = message.get();
				this.index = message.getInt();
				this.blockOffset = message.getInt();
				this.payload = new byte[length-9];
				message.get(this.payload);
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
	
	public ByteBuffer sendMessage(int id, int piece, int offset, TorrentFile file){
		if(id == 6){
			this.blockOffset = offset;
			this.piece = piece;
			this.file = file;
			
			return sendRequest();
		}
		return null;
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
	
	private ByteBuffer sendBitfield(){
		length = 0;
		messageID = 5;
		return craft();
	}
	
	private ByteBuffer sendRequest(){
		length = 13; //payload length 12
		messageID = 6;
		this.payload = new byte[length-1];
		ByteBuffer buf = ByteBuffer.allocate(length-1);
		buf.putInt(piece);
		buf.putInt(this.blockOffset);
		if (REQUEST_SIZE > (file.getFileSize() - file.bytesReceived)){
			buf.putInt((int) (file.getFileSize() % REQUEST_SIZE));
		}
		//buf.putInt(remaining);
		else {buf.putInt(PeerMessage.REQUEST_SIZE);}
		this.payload = buf.array();
		//System.out.println("in sendRequest payload: "+Arrays.toString(payload));
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