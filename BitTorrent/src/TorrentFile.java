import java.io.File;
import java.io.IOException;
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
		public enum Message { CHOKE(0) , UNCHOKE(1), INTERESTED(2), 
			UNINTERESTED(3), HAVE(4), BITFIELD(5), REQUEST(6), PIECE(7), CANCEL(8); 
			
			private int val;
			Message(int val){
				this.val = val;
			}
		}
		
		//actual fields
		private int length;
		private int messageID;
		private byte[] payload;
		private int index;
		private int blockOffset;
		private int blockOther;
		
		public PeerMessage(ByteBuffer message){
			byte[] field = new byte[1]; 
			message.get(field);
			length = field[0]; //payload length
			field = new byte[1];
			message.get(field); 
			messageID = field[0]; //messageID
			//payload = new byte[length]; //get that payload ready
			field = new byte[length];
			message.get(field); //put the payload stuff into its own array, dang it
			if(messageID >= Message.HAVE.val)
				parse(message);
		}
		
		//parse the messages that actually have a payload
		private void parse(ByteBuffer message){
			if(messageID == Message.HAVE.val){
				//id 4, payload of 4
				//payload is a number denoting the index of a piece 
				//that the peer has successfully downloaded and validated
				
			}
			else if (messageID == Message.BITFIELD.val){
				
			}
			else if (messageID == Message.REQUEST.val){
				
			}
			else if (messageID == Message.PIECE.val){
				//received a piece, so download it and move on to the next offset
				//you know, assuming it's the one we're trying to download
				//REFERENCE
				//  Pieces......: 1294 piece(s) (262144 byte(s)/piece)
				//  Total size..: 339006116 byte(s)
				
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
			}
			else if (messageID == Message.CANCEL.val){
				
			}
		}
		
		public PeerMessage sendMessage(int id){
			if (messageID == Message.BITFIELD.val)
				return sendBitfield();
			else if (messageID == Message.REQUEST.val)
				return sendRequest();
			else if (messageID == Message.CHOKE.val)
				return sendChoke();
			else if (messageID == Message.UNCHOKE.val)
				return sendUnchoke();
			else if (messageID == Message.INTERESTED.val)
				return sendInterested();
			else if (messageID == Message.UNINTERESTED.val)
				return sendUninterested();
			else if (messageID == Message.HAVE.val)
				return sendHave();
			else if (messageID == Message.PIECE.val)
				return sendPiece();
			else if (messageID == Message.CANCEL.val)
				return sendCancel();
			return null;
		}
		
		private PeerMessage sendChoke(){
			return null;
		}
		
		private PeerMessage sendUnchoke(){
			return null;
		}
		
		private PeerMessage sendInterested(){
			return null;
		}
		
		private PeerMessage sendUninterested(){
			return null;
		}
		
		private PeerMessage sendHave(){
			return null;
		}
		
		private PeerMessage sendPiece(){
			return null;
		}
		
		private PeerMessage sendCancel(){
			return null;
		}
		
		private PeerMessage sendRequest(){
			return null;
		}
		
		private PeerMessage sendBitfield(){
			return null;
		}
	}
	
}


