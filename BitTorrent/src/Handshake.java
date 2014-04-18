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
		this.dataBytes.rewind();
	}
	
	public byte[] getDataBytes() {
        return this.dataBytes.array();
}

	public byte[] getInfoHash() {
		return this.infoHash.array();
	}

	public byte[] getPeerId() {
		return this.peerId.array();
	}

	public static Handshake parse(ByteBuffer buffer)
		throws ParseException, UnsupportedEncodingException {
		
		int protocolStrLen = Byte.valueOf(buffer.get()).intValue();
        if (protocolStrLen < 0 ||
                        buffer.remaining() != 67) {
                throw new ParseException("Incorrect handshake message length " +
                           "(protocolStrLen=" + protocolStrLen + ") !", 0);
        }

        // Check the protocol identification string
        byte[] pstr = new byte[protocolStrLen];
        buffer.get(pstr);

        if (!Handshake.PROTOCOL_NAME.equals(
                                new String(pstr, Torrent.BYTE_ENCODING))) {
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

	public static Handshake make(byte[] torrentInfoHash,
			byte[] clientPeerId) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(
					Handshake.LENGTH); //fix later

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


	
}
