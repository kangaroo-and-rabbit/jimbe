package org.atriasoft.jimbe.matroska.model;

import java.nio.ByteBuffer;

import org.atriasoft.jimbe.ebml.model.BinaryElement;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement;
import org.atriasoft.jimbe.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the Matroska SeekHead element, which indexes the positions of level-1 elements
 * within a Segment for fast seeking.
 * <p>
 * The MetaSeek reserves space in the output stream when first written, allowing subsequent
 * additions of indexed elements. After adding new entries, the {@link #update(DataWriter)}
 * method seeks back and rewrites the SeekHead in place.
 * </p>
 */
public class MetaSeek {
	private static final Logger LOG = LoggerFactory.getLogger(MetaSeek.class);
	private static final long BLOCK_RESERVE_SIZE = 256;
	private final long myPosition;
	private final MasterElement seekHeadElem;
	private final VoidElement placeHolderElem;
	private final long referencePosition;

	/**
	 * Creates a MetaSeek object to index the positions of level-1 elements.
	 *
	 * @param referencePosition the byte position of the first byte after the Segment element header
	 */
	public MetaSeek(final long referencePosition) {
		this.referencePosition = referencePosition;
		this.myPosition = referencePosition;
		this.seekHeadElem = DocTypes.SeekHead.getInstance();
		this.placeHolderElem = new VoidElement(BLOCK_RESERVE_SIZE - this.seekHeadElem.getTotalSize());
	}

	/**
	 * Writes this object into the data stream at the current position. Note that this method reserves some space for further additions in the stream,
	 * if the stream is seekable. Following subsequent additions to the object, the update() method can be used to update the originally written object.
	 *
	 * @param ioDW data stream to write to
	 * @return length of data written
	 */
	public long write(final DataWriter ioDW) {
		final long elemLen = this.seekHeadElem.writeElement(ioDW);
		final long voidLen = this.placeHolderElem.writeElement(ioDW);
		assert elemLen + voidLen == BLOCK_RESERVE_SIZE;
		return BLOCK_RESERVE_SIZE;
	}

	/**
	 * Updates the representation of the object in the datastream to account for added indexed elements
	 *
	 * @param ioDW the data stream containing this object
	 */
	public void update(final DataWriter ioDW) {
		assert ioDW.isSeekable();
		final long pos = ioDW.getFilePointer();
		ioDW.seek(this.myPosition);
		write(ioDW);
		ioDW.seek(pos);
		LOG.debug("Updated metaseek section.");
	}

	/**
	 * Adds elements to the seek index. These should be level 1 objects only. If this object has already been written, you must use the update() method
	 * for changes to take effect.
	 *
	 * @param element The element itself
	 * @param filePosition Position in the data stream where the element has been written.
	 */
	public void addIndexedElement(final Element element, final long filePosition) {
		LOG.debug("Adding indexed element {} @ {}", element.getElementType().getName(),
				filePosition - this.referencePosition);
		addIndexedElement(element.getType(), filePosition);
	}

	/**
	 * Adds an element to the seek index by its raw type bytes. These should be level-1 elements only.
	 * If this object has already been written, use {@link #update(DataWriter)} for changes to take effect.
	 *
	 * @param elementType the raw EBML element type ID bytes
	 * @param filePosition the absolute byte position where the element was written
	 */
	public void addIndexedElement(final ByteBuffer elementType, final long filePosition) {
		LOG.debug("Adding indexed element @ {}", filePosition - this.referencePosition);
		final MasterElement seekEntryElem = DocTypes.Seek.getInstance();
		final BinaryElement seekEntryIdElem = DocTypes.SeekID.getInstance();
		seekEntryIdElem.setData(elementType);

		final UnsignedIntegerElement seekEntryPosElem = DocTypes.SeekPosition.getInstance();
		seekEntryPosElem.setValue(filePosition - this.referencePosition);

		seekEntryElem.addChildElement(seekEntryIdElem);
		seekEntryElem.addChildElement(seekEntryPosElem);

		this.seekHeadElem.addChildElement(seekEntryElem);
		this.placeHolderElem.setSize(BLOCK_RESERVE_SIZE - this.seekHeadElem.getTotalSize());
	}
}
