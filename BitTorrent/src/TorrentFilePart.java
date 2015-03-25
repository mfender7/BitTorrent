import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.turn.ttorrent.common.Torrent;

public class TorrentFilePart {

	public static final int REQUEST_SIZE = 16384;

	RandomAccessFile af; // = new RandomAccessFile("", "rw");
	FileChannel channel;

	private int piece;
	private int offset;
	private ByteBuffer data;
	private TorrentFile file;
	private boolean complete;

	public TorrentFilePart(TorrentFile file, int piece, int offset,
			int pieceLength) {
		this.file = file;
		this.piece = piece;
		this.offset = offset;
		this.data = ByteBuffer.allocate(pieceLength);
		this.complete = false;
	}
	
	public void createFile(Torrent torrent) throws FileNotFoundException{
		af = new RandomAccessFile(new File(""), "");
		channel = af.getChannel();
		//LET'S DO THIS
		
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public int getOffset() {
		return this.offset;
	}

	public void add(byte[] d, int offset) {
		data.put(d);
		this.offset = offset;
	}

	public void write(ByteBuffer data, int piece, int offset) {
		if (this.piece == piece)
			this.data.put(data);

	}
	
	public ByteBuffer getData(){
		return data;
	}
}
