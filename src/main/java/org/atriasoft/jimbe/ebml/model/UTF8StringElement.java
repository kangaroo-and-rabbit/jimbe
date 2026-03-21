package org.atriasoft.jimbe.ebml.model;

import java.nio.charset.StandardCharsets;

/**
 * EBML string element using UTF-8 encoding.
 */
public class UTF8StringElement extends StringElement {
	public UTF8StringElement() {
		super(StandardCharsets.UTF_8);
	}
}
