import com.turn.ttorrent.common.*;
import java.io.*;

public class App {
	public static void main(String[] args){
		System.out.println("Go go go!");
		Torrent torrent = null;
		try{			
			torrent = Torrent.load(new File("test.mkv.torrent"));
			torrent = Torrent.load(new File("test2.torrent"));
		}
		catch (Exception ex){
			System.out.println("Aww... why don't you like me, File?");
			System.out.println(ex.getMessage());
		}
	}
}
