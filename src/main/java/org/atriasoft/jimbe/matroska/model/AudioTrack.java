package org.atriasoft.jimbe.matroska.model;

/**
 * Holds the audio-specific properties of a Matroska track, such as sampling frequency,
 * number of channels, and bit depth.
 *
 * @see Track
 */
public class AudioTrack {
	private float samplingFrequency;
	private float outputSamplingFrequency;
	private short channels;
	private byte bitDepth;

	/** Returns the sampling frequency in Hz. */
	public float getSamplingFrequency() {
		return this.samplingFrequency;
	}

	/** Sets the sampling frequency in Hz. */
	public void setSamplingFrequency(final float samplingFrequency) {
		this.samplingFrequency = samplingFrequency;
	}

	/** Returns the output sampling frequency in Hz (used after SBR processing), or 0 if not set. */
	public float getOutputSamplingFrequency() {
		return this.outputSamplingFrequency;
	}

	/** Sets the output sampling frequency in Hz. */
	public void setOutputSamplingFrequency(final float outputSamplingFrequency) {
		this.outputSamplingFrequency = outputSamplingFrequency;
	}

	/** Returns the number of audio channels. */
	public short getChannels() {
		return this.channels;
	}

	/** Sets the number of audio channels. */
	public void setChannels(final short channels) {
		this.channels = channels;
	}

	/** Returns the bit depth per sample, or 0 if not set. */
	public byte getBitDepth() {
		return this.bitDepth;
	}

	/** Sets the bit depth per sample. */
	public void setBitDepth(final int bitDepth) {
		this.bitDepth = (byte) bitDepth;
	}
}
