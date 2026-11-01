/**
 * JEBML - Java library to read/write EBML/Matroska elements.
 * Copyright (C) 2004 Jory Stone <jebml@jory.info>
 * Based on Javatroska (C) 2002 John Cannon <spyder@matroska.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.atriasoft.jimbe.matroska.model;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.atriasoft.jimbe.ebml.Reader;
import org.atriasoft.jimbe.ebml.model.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Matroska Block element used for reading frame data from a container.
 * <p>
 * A Block contains one or more frames of media data for a single track at a given timecode.
 * It supports multiple lacing modes (Xiph, EBML, and fixed-size) for packing
 * multiple frames into a single block. This class handles parsing the block header,
 * extracting lace sizes, and providing access to individual frames.
 * </p>
 *
 * @see SimpleBlock
 * @see Frame
 * @see LaceMode
 */
public class Block {
	private static final Logger LOG = LoggerFactory.getLogger(Block.class);

	protected int[] sizes = null;
	protected int headerSize = 0;
	protected int blockTimecode = 0;
	protected int trackNo = 0;
	private boolean keyFrame;
	private final ByteBuffer data;

	/**
	 * Constructs a Block from raw binary data. The buffer is duplicated: parsing shares its content
	 * but leaves its position untouched.
	 *
	 * @param data the raw block data including header and frame payload
	 */
	public Block(final ByteBuffer data) {
		LOG.trace("Block created with data {}, {}", data.limit(), data.position());
		this.data = data.duplicate();
	}

	/**
	 * Parses the block header to extract the track number, timecode, key frame flag,
	 * lacing mode, and individual frame sizes.
	 *
	 * @throws RuntimeException if an unsupported lacing type is encountered
	 */
	public void parseBlock() {
		int index = 0;
		this.trackNo = (int) Reader.readEBMLCode(this.data);
		index = Element.codedSizeLength(this.trackNo, 0);
		this.headerSize += index;

		this.blockTimecode = this.data.getShort();
		LOG.trace("Block belongs to track {} @ {}", this.trackNo, this.blockTimecode);

		final byte flagsByte = this.data.get();
		final int keyFlag = flagsByte & 0x80;
		if (keyFlag > 0) {
			this.keyFrame = true;
		} else {
			this.keyFrame = false;
		}

		final int laceFlag = flagsByte & 0x06;
		index++;
		// Increase the HeaderSize by the number of bytes we have read
		this.headerSize += 3;
		if (laceFlag != 0x00) {
			// We have lacing
			final byte laceCount = this.data.get();
			this.headerSize += 1;
			if (laceFlag == 0x02) { // Xiph Lacing
				LOG.trace("Reading xiph lace sizes");
				this.sizes = readXiphLaceSizes(index, laceCount);
			} else if (laceFlag == 0x06) { // EBML Lacing
				LOG.trace("Reading ebml lace sizes");
				this.sizes = readEBMLLaceSizes(index, laceCount);
			} else if (laceFlag == 0x04) { // Fixed Size Lacing
				LOG.trace("Fixed lace sizes");
				this.sizes = new int[laceCount + 1];
				this.sizes[0] = this.data.remaining() / (laceCount + 1);
				for (int s = 0; s < laceCount; s++) {
					this.sizes[s + 1] = this.sizes[0];
				}
			} else {
				throw new RuntimeException("Unsupported lacing type flag.");
			}
			LOG.trace("Lace sizes: {}", Arrays.toString(this.sizes));
		}
		// data = new byte[(int)(this.getSize() - HeaderSize)];
		// source.read(data, 0, data.length);
		// this.dataRead = true;
		this.headerSize = this.data.position();
	}

	private int[] readEBMLLaceSizes(int index, final short laceCount) {
		final int[] laceSizes = new int[laceCount + 1];
		laceSizes[laceCount] = this.data.remaining();

		// This uses the DataSource.getBytePosition() for finding the header size
		// because of the trouble of finding the byte size of sized ebml coded integers
		// long ByteStartPos = source.getFilePointer();
		final int startIndex = index;

		laceSizes[0] = (int) Reader.readEBMLCode(this.data);
		index += Element.codedSizeLength(laceSizes[0], 0);
		laceSizes[laceCount] -= laceSizes[0];

		long firstEBMLSize = laceSizes[0];
		long lastEBMLSize = 0;
		for (int l = 0; l < laceCount - 1; l++) {
			lastEBMLSize = Reader.readSignedEBMLCode(this.data);
			index += Element.codedSizeLength(lastEBMLSize, 0);

			firstEBMLSize += lastEBMLSize;
			laceSizes[l + 1] = (int) firstEBMLSize;

			// Update the size of the last block
			laceSizes[laceCount] -= laceSizes[l + 1];
		}
		// long ByteEndPos = source.getFilePointer();

		// HeaderSize = HeaderSize + (int)(ByteEndPos - ByteStartPos);
		this.headerSize = this.headerSize + index - startIndex;
		laceSizes[laceCount] -= this.headerSize;

		return laceSizes;
	}

	private int[] readXiphLaceSizes(final int index, final short laceCount) {
		final int[] laceSizes = new int[laceCount + 1];
		laceSizes[laceCount] = this.data.remaining();

		// long ByteStartPos = source.getFilePointer();

		for (int l = 0; l < laceCount; l++) {
			short laceSizeByte = 255;
			while (laceSizeByte == 255) {
				laceSizeByte = (short) (this.data.get() & 0xFF);
				this.headerSize += 1;
				laceSizes[l] += laceSizeByte;
			}
			// Update the size of the last block
			laceSizes[laceCount] -= laceSizes[l];
		}
		// long ByteEndPos = source.getFilePointer();

		laceSizes[laceCount] -= this.headerSize;

		return laceSizes;
	}

	/**
	 * Returns the number of frames contained in this block.
	 *
	 * @return the frame count (1 if no lacing is used, otherwise the number of laced frames)
	 */
	public int getFrameCount() {
		if (this.sizes == null) {
			return 1;
		}
		return this.sizes.length;
	}

	/**
	 * Returns the data of a specific frame within this block.
	 *
	 * @param frame the zero-based index of the frame to retrieve
	 * @return a {@link ByteBuffer} containing the frame data
	 * @throws IllegalArgumentException if {@code frame} is greater than 0 on a non-laced block
	 */
	public ByteBuffer getFrame(final int frame) {
		int startOffset = this.headerSize;
		int endOffset;
		if (this.sizes == null) {
			if (frame != 0) {
				throw new IllegalArgumentException(
						"Tried to read laced frame on non-laced Block. Block.getFrame(frame > 0)");
			}
			endOffset = this.headerSize + this.data.remaining();
		} else {
			// Calc the frame data offset
			for (int s = 0; s < frame; s++) {
				startOffset += this.sizes[s];
			}
			endOffset = this.sizes[frame] + startOffset;
		}

		// Copy the frame data
		final ByteBuffer frameData = this.data.duplicate();
		frameData.position(startOffset);
		frameData.limit(endOffset);
		return frameData;
	}

	/**
	 * Computes the absolute timecode of this block by adding the cluster timecode to the block-relative timecode.
	 *
	 * @param clusterTimecode the timecode of the containing cluster
	 * @param timecodeScale the timecode scale (currently unused in the calculation)
	 * @return the adjusted absolute timecode
	 */
	public long getAdjustedBlockTimecode(final long clusterTimecode, final long timecodeScale) {
		return clusterTimecode + (this.blockTimecode); // * timecodeScale);
	}

	/** Returns the track number this block belongs to. */
	public int getTrackNo() {
		return this.trackNo;
	}

	/** Returns the block-relative timecode (offset from the cluster timecode). */
	public int getBlockTimecode() {
		return this.blockTimecode;
	}

	/**
	 * Sets the frame data for this block (currently not implemented).
	 *
	 * @param trackNo the track number
	 * @param timecode the timecode
	 * @param data the raw frame data
	 */
	public void setFrameData(final short trackNo, final int timecode, final byte[] data) {

	}

	/** Returns {@code true} if this block contains a key frame. */
	public boolean isKeyFrame() {
		return this.keyFrame;
	}
}
