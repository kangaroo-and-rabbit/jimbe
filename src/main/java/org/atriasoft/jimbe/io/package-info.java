/**
 * I/O interfaces and file-based implementations for reading and writing binary data.
 *
 * <p>The core interfaces are:</p>
 * <ul>
 *     <li>{@link org.atriasoft.jimbe.io.DataSource} &mdash; reading bytes and seeking in input data</li>
 *     <li>{@link org.atriasoft.jimbe.io.DataWriter} &mdash; writing bytes and seeking in output data</li>
 *     <li>{@link org.atriasoft.jimbe.io.DataSeekable} &mdash; common base for position, length, and seeking</li>
 * </ul>
 *
 * <p>File-based implementations ({@link org.atriasoft.jimbe.io.FileDataSource},
 * {@link org.atriasoft.jimbe.io.FileDataWriter}) use {@link java.io.RandomAccessFile}
 * and {@link java.nio.channels.FileChannel} for efficient I/O.</p>
 *
 * <p>To use a custom data source (e.g. in-memory, network stream), implement
 * {@link org.atriasoft.jimbe.io.DataSource} or {@link org.atriasoft.jimbe.io.DataWriter}.</p>
 */
package org.atriasoft.jimbe.io;
