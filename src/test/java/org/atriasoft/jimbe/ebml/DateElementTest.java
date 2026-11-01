package org.atriasoft.jimbe.ebml;

import java.util.Date;

import org.atriasoft.jimbe.ebml.model.DateElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateElementTest {

	// Use 4-byte IDs in unused range to avoid collision with DocTypes
	private static final byte[] DATE_ID = { (byte) 0x1F, (byte) 0xFE, (byte) 0xF3, (byte) 0x01 };

	@Test
	public void testUnixEpochDelayConstant() {
		// EBML epoch is 2001-01-01 00:00:00 UTC = 978307200 seconds after Unix epoch
		Assertions.assertEquals(978307200L, DateElement.UnixEpochDelay);
	}

	@Test
	public void testAlwaysUses8Bytes() {
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		elem.setDate(new Date(0));
		Assertions.assertEquals(8, elem.getSize());
	}

	@Test
	public void testSetValueAlso8Bytes() {
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		elem.setValue(0L);
		Assertions.assertEquals(8, elem.getSize());
	}

	@Test
	public void testSetAndGetDateRoundTrip() {
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		final Date input = new Date(1716163200123L); // 2024-05-20T00:00:00.123Z
		elem.setDate(input);
		Assertions.assertEquals(input, elem.getDate());
	}

	@Test
	public void testEbmlEpochIsZero() {
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		elem.setDate(new Date(978307200000L));
		Assertions.assertEquals(0L, elem.getValue());
	}

	@Test
	public void testDateIsStoredInNanoseconds() {
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		elem.setDate(new Date(978307201000L)); // one second after the EBML epoch
		Assertions.assertEquals(1000000000L, elem.getValue());
	}

	@Test
	public void testDateBeforeTheEbmlEpoch() {
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		elem.setDate(new Date(0));
		Assertions.assertEquals(-978307200000000000L, elem.getValue());
		Assertions.assertEquals(new Date(0), elem.getDate());
	}

	@Test
	public void testReadsADateWrittenElsewhere() {
		// 2024-05-20T00:00:00Z as an EBML date, the value another muxer stores.
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		elem.setValue((1716163200000L - 978307200000L) * 1000000L);
		Assertions.assertEquals(new Date(1716163200000L), elem.getDate());
	}

	@Test
	public void testSetValueRawNanoseconds() {
		// setValue/getValue work with raw nanoseconds (the EBML date format)
		final DateElement elem = new DateElement();
		elem.setType(DATE_ID);
		final long nanos = 1000000000L; // 1 second in nanoseconds
		elem.setValue(nanos);
		Assertions.assertEquals(nanos, elem.getValue());
	}

	@Test
	public void testConstructorWithType() {
		final DateElement elem = new DateElement(DATE_ID);
		elem.setValue(0L);
		Assertions.assertEquals(8, elem.getSize());
		Assertions.assertEquals(0L, elem.getValue());
	}
}
