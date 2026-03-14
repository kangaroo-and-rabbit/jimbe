package org.ebml.matroska;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MatroskaFileFrameTest {

	@Test
	public void testDefaultDuration() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		Assertions.assertEquals(Long.MIN_VALUE, frame.getDuration());
	}

	@Test
	public void testSetGetTrackNo() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTrackNo(42);
		Assertions.assertEquals(42, frame.getTrackNo());
	}

	@Test
	public void testSetGetTimecode() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setTimecode(1234567890L);
		Assertions.assertEquals(1234567890L, frame.getTimecode());
	}

	@Test
	public void testSetGetDuration() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setDuration(33);
		Assertions.assertEquals(33, frame.getDuration());
	}

	@Test
	public void testSetGetKeyFrame() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		Assertions.assertFalse(frame.isKeyFrame());
		frame.setKeyFrame(true);
		Assertions.assertTrue(frame.isKeyFrame());
	}

	@Test
	public void testSetDataStoresDuplicate() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		final ByteBuffer original = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
		frame.setData(original);
		original.get();
		final ByteBuffer retrieved = frame.getData();
		Assertions.assertEquals(0, retrieved.position());
		Assertions.assertEquals(5, retrieved.remaining());
	}

	@Test
	public void testGetDataReturnsDuplicate() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.setData(ByteBuffer.wrap(new byte[]{10, 20, 30}));
		final ByteBuffer data1 = frame.getData();
		data1.get();
		final ByteBuffer data2 = frame.getData();
		Assertions.assertEquals(0, data2.position());
	}

	@Test
	public void testAddReferences() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.addReferences(100L, 200L, 300L);
		Assertions.assertEquals(3, frame.getReferences().size());
		Assertions.assertEquals(100L, frame.getReferences().get(0));
		Assertions.assertEquals(200L, frame.getReferences().get(1));
		Assertions.assertEquals(300L, frame.getReferences().get(2));
	}

	@Test
	public void testAddReferencesAccumulates() {
		final MatroskaFileFrame frame = new MatroskaFileFrame();
		frame.addReferences(100L);
		frame.addReferences(200L);
		Assertions.assertEquals(2, frame.getReferences().size());
	}

	@Test
	public void testCopyConstructor() {
		final MatroskaFileFrame original = new MatroskaFileFrame();
		original.setTrackNo(5);
		original.setTimecode(1000);
		original.setDuration(33);
		original.setKeyFrame(true);
		original.setData(ByteBuffer.wrap(new byte[]{1, 2, 3}));
		original.addReferences(42L, 84L);

		final MatroskaFileFrame copy = new MatroskaFileFrame(original);
		Assertions.assertEquals(5, copy.getTrackNo());
		Assertions.assertEquals(1000, copy.getTimecode());
		Assertions.assertEquals(33, copy.getDuration());
		Assertions.assertTrue(copy.isKeyFrame());
		Assertions.assertEquals(3, copy.getData().remaining());
		Assertions.assertEquals(2, copy.getReferences().size());
		Assertions.assertEquals(42L, copy.getReferences().get(0));
	}

	@Test
	public void testCopyConstructorIndependentReferences() {
		final MatroskaFileFrame original = new MatroskaFileFrame();
		original.setData(ByteBuffer.wrap(new byte[]{1}));
		original.addReferences(10L);

		final MatroskaFileFrame copy = new MatroskaFileFrame(original);
		copy.addReferences(20L);

		Assertions.assertEquals(1, original.getReferences().size());
		Assertions.assertEquals(2, copy.getReferences().size());
	}
}
