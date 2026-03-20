package org.atriasoft.jimbe.matroska.model;

public class AudioTrack
{
  private float samplingFrequency;
  private float outputSamplingFrequency;
  private short channels;
  private byte bitDepth;

  public float getSamplingFrequency()
  {
    return samplingFrequency;
  }

  public void setSamplingFrequency(final float samplingFrequency)
  {
    this.samplingFrequency = samplingFrequency;
  }

  public float getOutputSamplingFrequency()
  {
    return outputSamplingFrequency;
  }

  public void setOutputSamplingFrequency(final float outputSamplingFrequency)
  {
    this.outputSamplingFrequency = outputSamplingFrequency;
  }

  public short getChannels()
  {
    return channels;
  }

  public void setChannels(final short channels)
  {
    this.channels = channels;
  }

  public byte getBitDepth()
  {
    return bitDepth;
  }

  public void setBitDepth(final int bitDepth)
  {
    this.bitDepth = (byte) bitDepth;
  }
}
