/**
 * High-level Matroska/WebM file reader and writer.
 *
 * <p>{@link org.atriasoft.jimbe.matroska.Reader} parses a complete Matroska file:
 * EBML header validation, segment info, track descriptors, tags, and clusters.
 * It provides frame-by-frame access via {@link org.atriasoft.jimbe.matroska.Reader#getNextFrame()}
 * and seeking via {@link org.atriasoft.jimbe.matroska.Reader#seek(long)}.</p>
 *
 * <p>{@link org.atriasoft.jimbe.matroska.Writer} creates Matroska files with automatic
 * cluster management, cue point generation, and meta-seek index.</p>
 *
 * <p>The data model classes (Track, Frame, etc.) are in
 * {@link org.atriasoft.jimbe.matroska.model}.</p>
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * FileDataSource source = new FileDataSource("video.mkv");
 * Reader reader = new Reader(source);
 * reader.readFile();
 * Frame frame;
 * while ((frame = reader.getNextFrame()) != null) {
 *     // process frame
 * }
 * }</pre>
 */
package org.atriasoft.jimbe.matroska;
