package org.atriasoft.jimbe.ebml.model;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.atriasoft.jimbe.ebml.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Element prototype descriptor and factory. Each {@code ProtoType} registers an EBML element ID
 * with its name, nesting level, and concrete {@link Element} subclass. The static registry
 * allows looking up element types by their binary ID during parsing.
 *
 * @param <T> the concrete element type this prototype creates
 */
public class ProtoType<T extends Element> {
	private static final Logger LOG = LoggerFactory.getLogger(ProtoType.class);
	private static final HashMap<Long, ProtoType<? extends Element>> CLASS_MAP = new HashMap<>();
	Class<T> clazz;
	private final ByteBuffer type;

	private final String name;
	private final int level;

	/**
	 * Creates and registers a new element prototype.
	 *
	 * @param clazz the concrete element class to instantiate
	 * @param name the human-readable element name
	 * @param type the EBML element ID bytes
	 * @param level the nesting level in the EBML hierarchy
	 */
	public ProtoType(final Class<T> clazz, final String name, final byte[] type, final int level) {
		this.clazz = clazz;
		this.type = ByteBuffer.wrap(type);
		this.name = name;
		this.level = level;
		final long codename = Reader.parseEBMLCode(this.type);
		CLASS_MAP.put(codename, this);
		LOG.trace("Associating {} with {}", name, codename);
	}

	/**
	 * Creates a new element instance of this prototype's type, with type ID and prototype set.
	 *
	 * @return a new element instance
	 */
	public T getInstance() {
		LOG.trace("Instantiating {}", this.name);
		try {
			final T elem = this.clazz.newInstance();
			elem.setType(this.type);
			elem.setElementType(this);
			return elem;
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.error("Failed to instantiate: this should never happen!", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Looks up a registered prototype by its EBML element ID and creates an instance.
	 *
	 * @param type the EBML element ID bytes (with VINT marker bit)
	 * @return a new element instance, or {@code null} if the ID is unknown
	 */
	public static Element getInstance(final ByteBuffer type) {
		final long codename = Reader.parseEBMLCode(type);
		final ProtoType<? extends Element> eType = CLASS_MAP.get(Long.valueOf(codename));

		if (eType == null) {
			return null;
		}

		LOG.trace("Got codename {}, for element type {}", codename, eType.name);
		return eType.getInstance();
	}

	/**
	 * Returns the human-readable name of this element type.
	 *
	 * @return the element name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the nesting level of this element type in the EBML hierarchy.
	 *
	 * @return the nesting level (0 for top-level)
	 */
	public int getLevel() {
		return this.level;
	}

	/**
	 * Returns the EBML element ID bytes for this prototype.
	 *
	 * @return the type ID buffer
	 */
	public ByteBuffer getType() {
		return this.type;
	}

}
