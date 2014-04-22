import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

public class TorrentFileCreator {
	private RandomAccessFile af; // = new RandomAccessFile("", "rw");
	private FileChannel channel;
	private TorrentFile torrent;
	
	public TorrentFileCreator(TorrentFile torrent) throws FileNotFoundException{
		af = new RandomAccessFile(new File(torrent.getTorrent().getName()), "rw");
		channel = af.getChannel();
		this.torrent = torrent;
	}
	
	public void makeZeFile(Map<Integer, TorrentFilePart> parts) throws IOException{
		int i = 0;
		ByteBuffer buffer;
		while(i < torrent.getPieces()){
			buffer = parts.get(i).getData();
			buffer.flip();
			while(buffer.hasRemaining())
				channel.write(buffer);
			i += 1;
		}
		channel.close();
	}
}
