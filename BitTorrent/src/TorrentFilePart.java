import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class TorrentFilePart {
	
	public static final int REQUEST_SIZE = 16384;
	
	RandomAccessFile af; // = new RandomAccessFile("", "rw");
	FileChannel channel;
	
	private int piece;
	private int offset;
	private ByteBuffer data;
	private TorrentFile file;
	
	public TorrentFilePart(TorrentFile file, int piece, int offset, int pieceLength){
		this.file = file;
		this.piece = piece;
		this.offset = offset;
		data = ByteBuffer.allocate(pieceLength);
	}
	
	public void add(byte[] d, int offset){
		data.put(d);
		this.offset = offset;
	}
	
	public void write(ByteBuffer data, int piece, int offset){
		if(this.piece == piece)
			this.data.put(data);
		
	}
}
