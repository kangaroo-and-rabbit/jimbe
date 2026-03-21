/**
 * Low-level EBML (Extensible Binary Meta Language) reader and writer.
 *
 * <p>{@link org.atriasoft.jimbe.ebml.Reader} parses EBML element headers (ID + size)
 * from a {@link org.atriasoft.jimbe.io.DataSource}, resolves them via
 * {@link org.atriasoft.jimbe.ebml.model.ProtoType}, and returns typed
 * {@link org.atriasoft.jimbe.ebml.model.Element} instances. It also provides static
 * utility methods for EBML variable-length integer encoding and decoding.</p>
 *
 * <p>{@link org.atriasoft.jimbe.ebml.Writer} serializes elements (header + data) to a
 * {@link org.atriasoft.jimbe.io.DataWriter}.</p>
 *
 * <p>For higher-level Matroska/WebM file access, see
 * {@link org.atriasoft.jimbe.matroska}.</p>
 */
package org.atriasoft.jimbe.ebml;
