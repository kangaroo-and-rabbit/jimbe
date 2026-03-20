package org.atriasoft.ebml2;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MasterElementTest {

	// Use 4-byte IDs in unused range to avoid collision with DocTypes
	private static final byte[] MASTER_ID_1 = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF2, (byte) 0x01};
	private static final byte[] CHILD_ID_1 = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF2, (byte) 0x02};
	private static final byte[] CHILD_ID_2 = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF2, (byte) 0x03};
	private static final byte[] MASTER_ID_2 = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF2, (byte) 0x04};
	private static final byte[] INNER_ID = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF2, (byte) 0x05};
	private static final byte[] LEAF_ID = {(byte) 0x1F, (byte) 0xFE, (byte) 0xF2, (byte) 0x06};

	@Test
	public void testAddChildElementIncreasesSize() {
		final MasterElement master = new MasterElement();
		master.setType(MASTER_ID_1);
		Assertions.assertEquals(0, master.getSize());

		final BinaryElement child = new BinaryElement(CHILD_ID_1);
		child.setData(ByteBuffer.wrap(new byte[]{1, 2, 3}));

		master.addChildElement(child);
		Assertions.assertEquals(child.getTotalSize(), master.getSize());
	}

	@Test
	public void testAddMultipleChildElements() {
		final MasterElement master = new MasterElement();
		master.setType(MASTER_ID_1);

		final BinaryElement child1 = new BinaryElement(CHILD_ID_1);
		child1.setData(ByteBuffer.wrap(new byte[]{1, 2}));

		final BinaryElement child2 = new BinaryElement(CHILD_ID_2);
		child2.setData(ByteBuffer.wrap(new byte[]{3, 4, 5}));

		master.addChildElement(child1);
		master.addChildElement(child2);

		Assertions.assertEquals(child1.getTotalSize() + child2.getTotalSize(), master.getSize());
	}

	@Test
	public void testWriteAndReadChildren() {
		final ProtoType<MasterElement> masterType = new ProtoType<>(MasterElement.class, "TestMaster_MET",
				MASTER_ID_2, 1);
		final ProtoType<UnsignedIntegerElement> childType = new ProtoType<>(UnsignedIntegerElement.class, "TestChild_MET",
				CHILD_ID_1, 2);

		final MasterElement master = masterType.getInstance();
		final UnsignedIntegerElement child = childType.getInstance();
		child.setValue(42);
		master.addChildElement(child);

		final MockWriter writer = new MockWriter();
		master.writeElement(writer);

		final ByteBuffer written = writer.getBuff();
		written.flip();

		final MockSource src = new MockSource(written);
		final EBMLReader reader = new EBMLReader(src);
		final Element readMaster = reader.readNextElement();

		Assertions.assertNotNull(readMaster);
		Assertions.assertTrue(readMaster instanceof MasterElement);

		final Element readChild = ((MasterElement) readMaster).readNextChild(reader);
		Assertions.assertNotNull(readChild);
		Assertions.assertEquals("TestChild_MET", readChild.getElementType().getName());

		readChild.readData(src);
		Assertions.assertEquals(42, ((UnsignedIntegerElement) readChild).getValue());

		final Element noMore = ((MasterElement) readMaster).readNextChild(reader);
		Assertions.assertNull(noMore);
	}

	@Test
	public void testNestedMasterElements() {
		final ProtoType<MasterElement> outerType = new ProtoType<>(MasterElement.class, "Outer_MET",
				MASTER_ID_1, 0);
		final ProtoType<MasterElement> innerType = new ProtoType<>(MasterElement.class, "Inner_MET",
				INNER_ID, 1);
		final ProtoType<UnsignedIntegerElement> leafType = new ProtoType<>(UnsignedIntegerElement.class, "Leaf_MET",
				LEAF_ID, 2);

		final MasterElement outer = outerType.getInstance();
		final MasterElement inner = innerType.getInstance();
		final UnsignedIntegerElement leaf = leafType.getInstance();
		leaf.setValue(99);
		inner.addChildElement(leaf);
		outer.addChildElement(inner);

		final MockWriter writer = new MockWriter();
		outer.writeElement(writer);

		final ByteBuffer written = writer.getBuff();
		written.flip();

		final MockSource src = new MockSource(written);
		final EBMLReader reader = new EBMLReader(src);

		final Element readOuter = reader.readNextElement();
		Assertions.assertNotNull(readOuter);
		Assertions.assertTrue(readOuter instanceof MasterElement);

		final Element readInner = ((MasterElement) readOuter).readNextChild(reader);
		Assertions.assertNotNull(readInner);
		Assertions.assertTrue(readInner instanceof MasterElement);

		final Element readLeaf = ((MasterElement) readInner).readNextChild(reader);
		Assertions.assertNotNull(readLeaf);
		readLeaf.readData(src);
		Assertions.assertEquals(99, ((UnsignedIntegerElement) readLeaf).getValue());
	}
}
