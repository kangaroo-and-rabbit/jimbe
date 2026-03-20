package org.atriasoft.jimbe.matroska.model;

import java.util.ArrayList;

public class TrackOperation
{
  final ArrayList<Long> joinUIDs = new ArrayList<>();

  // TODO: support 3d track ops?

  public void addVirtualTrackPart(final long uid)
  {
    joinUIDs.add(uid);
  }
}
