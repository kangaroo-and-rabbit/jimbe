# Jimbe

Java library for reading and writing EBML (Extensible Binary Meta Language) and Matroska/WebM container files.

Jimbe is a modernized fork of [JEBML](https://github.com/Matroska-Org/jebml), restructured with clean package separation, simplified class naming, comprehensive unit tests for stabilization, and Javadoc documentation.

## Features

- Read and write Matroska (`.mkv`) and WebM files
- Low-level EBML element parsing and serialization
- Seekable playback with frame-level random access
- Track management (video, audio, subtitle, control)
- Tag and metadata support
- Pluggable I/O layer (file-based or custom `DataSource`/`DataWriter`)

## Package Structure

```
org.atriasoft.jimbe
  .io/                  I/O interfaces (DataSource, DataWriter) and file implementations
  .ebml/                EBML Reader and Writer
  .ebml.model/          EBML element types (Element, MasterElement, ProtoType, ...)
  .matroska/            Matroska Reader and Writer
  .matroska.model/      Matroska data model (Track, Frame, Cluster, ...)
```

## Installation

### Maven

```xml
<dependency>
    <groupId>org.atria-soft</groupId>
    <artifactId>jimbe</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### Reading a Matroska file

```java
import org.atriasoft.jimbe.io.FileDataSource;
import org.atriasoft.jimbe.matroska.Reader;
import org.atriasoft.jimbe.matroska.model.Frame;
import org.atriasoft.jimbe.matroska.model.Track;

FileDataSource source = new FileDataSource("video.mkv");
Reader reader = new Reader(source);
reader.readFile();

// List tracks
for (Track track : reader.getTrackList()) {
    System.out.println(track);
}

// Read frames sequentially
Frame frame;
while ((frame = reader.getNextFrame()) != null) {
    System.out.printf("Track %d, timecode %d, %d bytes%n",
        frame.getTrackNo(), frame.getTimecode(), frame.getData().remaining());
}
```

### Reading frames from a specific track

```java
Frame frame;
while ((frame = reader.getNextFrame(1)) != null) {
    // Process only track 1 frames
}
```

### Seeking

```java
if (reader.isSeekable()) {
    long actualTimecode = reader.seek(5000); // Seek to 5000ms
    Frame frame = reader.getNextFrame();     // First frame at or after seek point
}
```

### Getting file metadata

```java
System.out.println("Title: " + reader.getSegmentTitle());
System.out.println("Muxing app: " + reader.getMuxingApp());
System.out.println("Writing app: " + reader.getWritingApp());
System.out.println("Timecode scale: " + reader.getTimecodeScale());
System.out.println(reader.getReport()); // Full file summary
```

### Writing a Matroska file

```java
import org.atriasoft.jimbe.io.FileDataWriter;
import org.atriasoft.jimbe.matroska.Writer;
import org.atriasoft.jimbe.matroska.model.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

FileDataWriter output = new FileDataWriter("output.mkv");
Writer writer = new Writer(output);

// Define a track
Track track = new Track();
track.setTrackNo(1);
track.setTrackType(TrackType.SUBTITLE);
track.setCodecID("S_TEXT/UTF8");
track.setDefaultDuration(1000);
writer.addTrack(track);

// Write frames
Frame frame = new Frame();
frame.setTrackNo(1);
frame.setTimecode(0);
frame.setData(ByteBuffer.wrap("Hello!".getBytes(StandardCharsets.UTF_8)));
writer.addFrame(frame);

writer.close();
output.close();
```

### Writing with video track

```java
Track videoTrack = new Track();
videoTrack.setTrackNo(1);
videoTrack.setTrackType(TrackType.VIDEO);
videoTrack.setCodecID("V_VP8");
videoTrack.setDefaultDuration(33333333L); // ~30fps in nanoseconds

VideoTrack videoInfo = new VideoTrack();
videoInfo.setPixelWidth((short) 1920);
videoInfo.setPixelHeight((short) 1080);
videoTrack.setVideo(videoInfo);

writer.addTrack(videoTrack);
```

### Low-level EBML access

```java
import org.atriasoft.jimbe.ebml.Reader;
import org.atriasoft.jimbe.ebml.model.Element;
import org.atriasoft.jimbe.ebml.model.MasterElement;
import org.atriasoft.jimbe.io.FileDataSource;

FileDataSource source = new FileDataSource("file.mkv");
Reader reader = new Reader(source);

Element element = reader.readNextElement();
while (element != null) {
    System.out.printf("Element: %s, size: %d%n",
        element.getElementType().getName(), element.getSize());

    if (element instanceof MasterElement master) {
        Element child = master.readNextChild(reader);
        // traverse children...
    }
    element.skipData(source);
    element = reader.readNextElement();
}
```

## Building

```bash
mvn clean install
```

Requires Java 21+.

## License

LGPL v2 - see [LGPL.txt](LGPL.txt).

## Credits

- **Edouard Dupin** - Modern interface refactoring
- **Jory Stone** - Original Java EBML port (JEBML)
- **John Cannon** - Original Javatroska implementation
