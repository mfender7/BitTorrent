import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.bcodec.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class App {
	private static final String HTTP_ENCODING = "ISO-8859-1";
	private static Peer peer;
	private static Client client;

	public static URL buildAnnounceURL(URI announceURI, Torrent torrent) {
		//URL trackerAnnounceURL = this.torrentInfo.TRACKER_URL;
		String base = announceURI.toString();
		StringBuilder url = new StringBuilder(base);
		try {
			url.append(base.contains("?") ? "&" : "?");
			url.append("info_hash=");
			url.append(URLEncoder.encode(new String(torrent.getInfoHash(), HTTP_ENCODING), HTTP_ENCODING)).append("&peer_id=");
			url.append(URLEncoder.encode(new String(client.getID().getBytes(), HTTP_ENCODING), HTTP_ENCODING));
			url.append("&port=");
			url.append(51413);
			return new URL(url.toString());
			
			
		}
		catch (Exception e) { // 
			return null;
		}
	 }
	
	public static void main(String[] args){
		System.out.println("Go go go!");
		//-TO0042-
		//-MT001-
		String id = "-TO0042-" + UUID.randomUUID()
				.toString().split("-")[4];
		System.out.println(id);
		Torrent torrent = null;
		try{			
			torrent = Torrent.load(new File("dsl-4.4.10.iso.torrent"));
			client = new Client(InetAddress.getLocalHost(), torrent);
			URL announce = buildAnnounceURL(torrent.getAnnounceList().get(0).get(0), torrent);
			System.out.println(announce);
			//send announce get request
			URLConnection connection = announce.openConnection();
			//String charset = "UTF-8";
			//connection.setRequestProperty("Accept-Charset", charset);
			InputStream in = connection.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(in);
			ByteBuffer response = ByteBuffer.wrap(baos.toByteArray());
			BEValue decoded = BDecoder.bdecode(response);
			Map<String, BEValue> params = decoded.getMap();
			if(params.containsKey("peers"))
				System.out.println("Hurray! Peers! We're somewhere!");
			else if (params.containsKey("info_hash"))
				System.out.println("Info hash maybe?");
			else
				System.out.println("Shit");
			
			String encoding = connection.getContentEncoding();
			encoding = encoding == null ? "UTF-8" : encoding;
			String body = IOUtils.toString(in, HTTP_ENCODING);
			System.out.println(body);
		}
		catch (Exception ex){
			System.out.println("Aww... why don't you like me, File?");
			System.out.println(ex.getMessage());
		}
	}
}
