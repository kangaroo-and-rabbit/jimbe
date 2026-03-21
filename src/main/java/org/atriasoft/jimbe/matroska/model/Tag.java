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

/**
 * Represents a Matroska SimpleTag element, consisting of a name/value pair
 * with optional nested child tags.
 *
 * @see TagEntry
 */
public class Tag {
	public String name;
	public String value;
	public ArrayList<Tag> children = new ArrayList<>();

	/**
	 * Returns a string representation of this tag and its children, indented to the given depth.
	 *
	 * @param depth the indentation depth (number of tab levels)
	 * @return the formatted string representation
	 */
	public String toString(int depth) {
		StringBuilder s = new StringBuilder().append(new String());
		String depthIndent = new String();
		for (int d = 0; d < depth; d++) {
			depthIndent += "\t";
		}

		s.append(depthIndent).append("SimpleTag\n");
		s.append(depthIndent).append("\tName: ").append(this.name).append("\n");
		s.append(depthIndent).append("\tValue: ").append(this.value).append("\n");

		depth++;
		for (final Tag element : this.children) {
			s.append(element.toString(depth));
		}

		return s.toString();
	}
}
