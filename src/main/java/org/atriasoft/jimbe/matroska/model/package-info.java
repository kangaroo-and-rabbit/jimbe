/**
 * Matroska data model classes.
 *
 * <p>Key classes:</p>
 * <ul>
 *     <li>{@link org.atriasoft.jimbe.matroska.model.Track} &mdash; track descriptor (video, audio, subtitle)</li>
 *     <li>{@link org.atriasoft.jimbe.matroska.model.VideoTrack} &mdash; video track properties (resolution)</li>
 *     <li>{@link org.atriasoft.jimbe.matroska.model.AudioTrack} &mdash; audio track properties (sample rate, channels)</li>
 *     <li>{@link org.atriasoft.jimbe.matroska.model.TrackType} &mdash; track type enumeration</li>
 *     <li>{@link org.atriasoft.jimbe.matroska.model.Frame} &mdash; a decoded media frame with timecode and data</li>
 *     <li>{@link org.atriasoft.jimbe.matroska.model.Cluster} &mdash; cluster element for writing</li>
 *     <li>{@link org.atriasoft.jimbe.matroska.model.DocTypes} &mdash; static registry of all Matroska EBML element prototypes</li>
 * </ul>
 *
 * <p>For reading and writing, see {@link org.atriasoft.jimbe.matroska.Reader}
 * and {@link org.atriasoft.jimbe.matroska.Writer}.</p>
 */
package org.atriasoft.jimbe.matroska.model;
