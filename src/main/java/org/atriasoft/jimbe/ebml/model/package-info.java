/**
 * EBML element type hierarchy and prototype registry.
 *
 * <p>All EBML data is represented as elements. {@link org.atriasoft.jimbe.ebml.model.Element}
 * is the base class, with specialized subclasses for each EBML data type:</p>
 * <ul>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.BinaryElement} &mdash; raw binary data</li>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.MasterElement} &mdash; container with child elements</li>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.StringElement} &mdash; ASCII string</li>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.UTF8StringElement} &mdash; UTF-8 string</li>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.UnsignedIntegerElement} &mdash; unsigned integer</li>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.SignedIntegerElement} &mdash; signed integer</li>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.FloatElement} &mdash; 32-bit or 64-bit float</li>
 *     <li>{@link org.atriasoft.jimbe.ebml.model.DateElement} &mdash; date (nanoseconds since EBML epoch)</li>
 * </ul>
 *
 * <p>{@link org.atriasoft.jimbe.ebml.model.ProtoType} maps EBML element IDs to their
 * concrete types, enabling automatic element instantiation during parsing.</p>
 */
package org.atriasoft.jimbe.ebml.model;
