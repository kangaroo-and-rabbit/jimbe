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

import java.util.ArrayList;

public class TagEntry {
	public ArrayList<Long> trackUID = new ArrayList<>();
	public ArrayList<Long> chapterUID = new ArrayList<>();
	public ArrayList<Long> attachmentUID = new ArrayList<>();
	public ArrayList<Tag> simpleTags = new ArrayList<>();

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder().append(new String());

		if (this.trackUID.size() > 0) {
			s.append("\t\t").append("TrackUID: ").append(this.trackUID.toString()).append("\n");
		}
		if (this.chapterUID.size() > 0) {
			s.append("\t\t").append("ChapterUID: ").append(this.chapterUID.toString()).append("\n");
		}
		if (this.attachmentUID.size() > 0) {
			s.append("\t\t").append("AttachmentUID: ").append(this.attachmentUID.toString()).append("\n");
		}

		for (final Tag element : this.simpleTags) {
			s.append(element.toString(2));
		}

		return s.toString();
	}
}
