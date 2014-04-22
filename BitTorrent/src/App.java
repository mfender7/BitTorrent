//import com.turn.ttorrent.common.Peer;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.bcodec.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class App {
	private static final String HTTP_ENCODING = "ISO-8859-1";
	private static Client client;

	public static URL buildAnnounceURL(URI announceURI, Torrent torrent,
			Client client) {
		// URL trackerAnnounceURL = this.torrentInfo.TRACKER_URL;
		String base = announceURI.toString();
		StringBuilder url = new StringBuilder(base);
		try {
			url.append(base.contains("?") ? "&" : "?");
			url.append("info_hash=");
			url.append(URLEncoder.encode(new String(torrent.getInfoHash(),
					HTTP_ENCODING), HTTP_ENCODING));
			url.append("&peer_id=").append(
					URLEncoder.encode(new String(client.getID().getBytes(),
							HTTP_ENCODING), HTTP_ENCODING));
			url.append("&port=").append(client.getService().getPort());
			url.append("&downloaded=0");
			url.append("&uploaded=0");
			url.append("&left=339006116");
			url.append("&compact=").append(1);
			return new URL(url.toString());

		} catch (Exception e) { //
			return null;
		}
	}

	private static List<Peer> toPeerList(byte[] data)
			throws InvalidBEncodingException, UnknownHostException {
		if (data.length % 6 != 0) {
			throw new InvalidBEncodingException("Invalid peers "
					+ "binary information string!");
		}

		List<Peer> result = new LinkedList<Peer>();
		ByteBuffer peers = ByteBuffer.wrap(data);

		for (int i = 0; i < data.length / 6; i++) {
			byte[] ipBytes = new byte[4];
			peers.get(ipBytes);
			InetAddress ip = InetAddress.getByAddress(ipBytes);
			int port = (0xFF & (int) peers.get()) << 8
					| (0xFF & (int) peers.get());
			result.add(new Peer(new InetSocketAddress(ip, port)));
		}

		return result;
	}

	/*
	 * PSUEDO CODE - as I need some damn clarity to myself for this last part
	 * ~~~~~~~ load torrent file parse/grab torrent.metadata build
	 * torrent.metadata.annoucelist to the tracker grab list of peers from
	 * tracker for this torrent file create torrentfile.part loop { pick piece
	 * to download handshake with peer trade data verfiy part is done piece++ }
	 */

	public static void main(String[] args) {
		System.out.println("Go go go!");

		Torrent torrent = null;
		try {
			//dsl.iso.torrent
			//Win8ShareLoader.7z.torrent
			torrent = Torrent.load(new File("zip.torrent")); 
			client = new Client(InetAddress.getLocalHost(), torrent);
			System.out.println(client.getID());
			client.run();
			URL announce = buildAnnounceURL(torrent.getAnnounceList().get(0).get(0), torrent, client);
			System.out.println(announce);
			// send announce get request
			URLConnection connection = announce.openConnection();
			// String charset = "UTF-8";
			// connection.setRequestProperty("Accept-Charset", charset);
			InputStream in = connection.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(in);
			ByteBuffer response = ByteBuffer.wrap(baos.toByteArray());
			BEValue decoded = BDecoder.bdecode(response);
			Map<String, BEValue> params = decoded.getMap();
			if (params.containsKey("peers")) {
				List<Peer> peers = toPeerList(params.get("peers").getBytes());
				System.out.println("creating TorrentFile");
				TorrentFile file = new TorrentFile(torrent, client);
				//took out the while(pieces< file.getPieces()) cause it 
				//wasn't doing anything useful. especially since we loop through all files
				//in getPiecesFromPeer
				int peerIndex = 0;
				while (peerIndex < peers.size()) {
					try {
						System.out.println(String.format("Trying Peer[%d/%d]", peerIndex + 1, peers.size()));
						Peer p = peers.get(peerIndex);
						Peer peer = client.getService().connect(p);
						if (peer != null) { // then we can do stuffs
							System.out.println("Establish peer connection");

							if ((peer = file.establishPeer(peer)) == null) {
								peerIndex++;
								continue;
							} else {
								System.out.println("Go go go");
								// REQUEST SHIT
								int i = file.getPiecesFromPeer();
								if (i==0){System.out.println("well, shit.");}
								else {
									//combine the pieces
									System.out.println("we should combine now!");
								}
								
							}
						} else
							System.out.println(String.format("Peer[%d/%d] didn't work. *sigh*", peerIndex + 1, peers.size()));
					} catch (Exception ex) {
						ex.printStackTrace();
						System.out.println("Peer index " + peerIndex);
						System.out.println("Nooooo. Damn it. Now to try another peer for the piece...");
					}
					peerIndex += 1;
					if(peerIndex >= peers.size())
						peerIndex = 0;
				}

				Peer p = peers.get(0);
				Peer connected = client.getService().connect(p);

				if (connected != null) {
					System.out.println("ID! " + connected.getPeerId().array());

				} else {
					System.out.println("Something didn't come back like we wanted it to...");
				}
			} else if (params.containsKey("info_hash"))
				System.out.println("Info hash maybe?");
			else
				System.out.println("Shit");

			// client.run();
			baos.close();

		} catch (Exception ex) {
			System.out.println("Aww... why don't you like me, File?");
			System.out.println(ex.getMessage());
		}
	}
}
