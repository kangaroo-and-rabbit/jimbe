package org.atriasoft.jimbe.matroska.model;

public class VideoTrack
{
  private short pixelWidth;
  private short pixelHeight;
  private short displayWidth = 0;
  private short displayHeight = 0;

  public short getPixelWidth()
  {
    return pixelWidth;
  }

  public void setPixelWidth(final short pixelWidth)
  {
    this.pixelWidth = pixelWidth;
  }

  public short getPixelHeight()
  {
    return pixelHeight;
  }

  public void setPixelHeight(final short pixelHeight)
  {
    this.pixelHeight = pixelHeight;
  }

  public short getDisplayWidth()
  {
    return displayWidth;
  }

  public void setDisplayWidth(final short displayWidth)
  {
    this.displayWidth = displayWidth;
  }

  public short getDisplayHeight()
  {
    return displayHeight;
  }

  public void setDisplayHeight(final short displayHeight)
  {
    this.displayHeight = displayHeight;
  }
}
