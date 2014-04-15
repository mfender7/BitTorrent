import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;

import com.turn.ttorrent.common.Torrent;


public class Handshake {
	public static final int LENGTH = 68;
	public static final int NAME_LENGTH = 19;
	public static final String PROTOCOL_NAME = "BitTorrent protocol";

	ByteBuffer infoHash;
	ByteBuffer peerId;
	ByteBuffer dataBytes;

	private Handshake(ByteBuffer infoHash,
			ByteBuffer peerId, ByteBuffer dataBytes) {
		this.infoHash = infoHash;
		this.peerId = peerId;
		this.dataBytes = dataBytes;
	}
	
	public ByteBuffer getDataBytes() {
		return this.dataBytes;
	}

	public byte[] getInfoHash() {
		return this.infoHash.array();
	}

	public byte[] getPeerId() {
		return this.peerId.array();
	}

	public static Handshake parse(ByteBuffer buffer)
		throws ParseException, UnsupportedEncodingException {
		
		byte[] nameLength = new byte[1];
		buffer.get(nameLength);
		//check if nameLength==19?

		// Check the protocol name
		byte[] protocolName = new byte[NAME_LENGTH];
		buffer.get(protocolName);

		if (!Handshake.PROTOCOL_NAME.equals(new String(protocolName, Torrent.BYTE_ENCODING))) {
			throw new ParseException("Invalid protocol identifier!", 1);
		}

		// Ignore reserved bytes
		byte[] reserved = new byte[8];
		buffer.get(reserved);

		byte[] infoHash = new byte[20];
		buffer.get(infoHash);
		byte[] peerId = new byte[20];
		buffer.get(peerId);
		return new Handshake(ByteBuffer.wrap(infoHash),
				ByteBuffer.wrap(peerId), buffer);
	}

	public static Handshake craft(byte[] torrentInfoHash,
			byte[] clientPeerId) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(
					Handshake.LENGTH + NAME_LENGTH); //fix later

			byte[] reserved = new byte[8];
			ByteBuffer infoHash = ByteBuffer.wrap(torrentInfoHash);
			ByteBuffer peerId = ByteBuffer.wrap(clientPeerId);

			buffer.put((byte)NAME_LENGTH);
			buffer.put(Handshake
					.PROTOCOL_NAME.getBytes(Torrent.BYTE_ENCODING));
			buffer.put(reserved);
			buffer.put(infoHash);
			buffer.put(peerId);

			return new Handshake(infoHash, peerId, buffer);
		} catch (UnsupportedEncodingException uee) {
			return null;
		}
	}
	
	public static Handshake make(byte[] torrentInfoHash,
			byte[] clientPeerId){
				return null;
		

	}
}
