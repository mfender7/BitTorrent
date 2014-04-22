import java.nio.ByteBuffer;


public class TorrentFilePart {
	
	public static final int REQUEST_SIZE = 16384;
	
	private ByteBuffer piece;
	private long offset;
	private byte[] data;
	
	public TorrentFilePart(ByteBuffer piece, long offset){
		this.piece = piece;
		this.offset = offset;
	}
}
