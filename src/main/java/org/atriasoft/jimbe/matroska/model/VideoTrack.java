package org.atriasoft.jimbe.matroska.model;

/**
 * Holds the video-specific properties of a Matroska track, such as pixel dimensions
 * and display dimensions.
 *
 * @see Track
 */
public class VideoTrack {
	private short pixelWidth;
	private short pixelHeight;
	private short displayWidth = 0;
	private short displayHeight = 0;

	/** Returns the encoded pixel width of the video. */
	public short getPixelWidth() {
		return this.pixelWidth;
	}

	/** Sets the encoded pixel width of the video. */
	public void setPixelWidth(final short pixelWidth) {
		this.pixelWidth = pixelWidth;
	}

	/** Returns the encoded pixel height of the video. */
	public short getPixelHeight() {
		return this.pixelHeight;
	}

	/** Sets the encoded pixel height of the video. */
	public void setPixelHeight(final short pixelHeight) {
		this.pixelHeight = pixelHeight;
	}

	/** Returns the display width (the width the video should be rendered at), or 0 if not set. */
	public short getDisplayWidth() {
		return this.displayWidth;
	}

	/** Sets the display width. */
	public void setDisplayWidth(final short displayWidth) {
		this.displayWidth = displayWidth;
	}

	/** Returns the display height (the height the video should be rendered at), or 0 if not set. */
	public short getDisplayHeight() {
		return this.displayHeight;
	}

	/** Sets the display height. */
	public void setDisplayHeight(final short displayHeight) {
		this.displayHeight = displayHeight;
	}
}
