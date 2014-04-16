import com.turn.ttorrent.common.Torrent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map.Entry;
import java.util.*;

import org.apache.commons.io.IOUtils;

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
			url.append(URLEncoder.encode(client.getID(), HTTP_ENCODING));
			url.append("&port=");
			url.append(53553);
			return new URL(url.toString());
			
			
		}
		catch (Exception e) { // 
			return null;
		}
	 }
	
	public static void main(String[] args){
		System.out.println("Go go go!");
		String id = "-MT0001-" + UUID.randomUUID()
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
			InputStream response = connection.getInputStream();
			
			//int status = connection.getResponseCode();
			//System.out.println(status);
		for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
			    System.out.println(header.getKey() + "=" + header.getValue());
			}
			String encoding = connection.getContentEncoding();
			encoding = encoding == null ? "UTF-8" : encoding;
			String body = IOUtils.toString(response, HTTP_ENCODING);
			System.out.println(body);
		}
		catch (Exception ex){
			System.out.println("Aww... why don't you like me, File?");
			System.out.println(ex.getMessage());
		}
	}
}
