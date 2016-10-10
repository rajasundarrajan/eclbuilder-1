package org.hpccsystems.dsp.ramps.utils;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class StreamUtils {
    private StreamUtils() {
    }

    private static void readLines(RandomAccessFile inputFile, StringBuilder outputStream, long seek) throws IOException {
        inputFile.seek(seek);
        String line = inputFile.readLine();
        while (line != null) {
            outputStream.append(line);
            outputStream.append("\n");
            line = inputFile.readLine();
        }
    }

    public static void tailFile(RandomAccessFile inputFile, StringBuilder outputStream, int bufferSize, int numLines)
            throws IOException {
        illegalArgumentExceptionCheck(bufferSize, numLines);
        byte[] buf = new byte[bufferSize];
        long fileSizeMark = inputFile.length();
        long offset = fileSizeMark;
        long seek;
        int len;
        int lines=numLines;
        if (offset - bufferSize >= 0) {
            seek = offset - bufferSize;
            len = bufferSize;
        } else {
            seek = 0;
            len = (int) offset;
        }
        inputFile.seek(seek);
        int bytesRead = inputFile.read(buf, 0, len);
        while (bytesRead != -1 && bytesRead != 0) {
            int i = bytesRead - 1;
            while (i >= 0 && lines > 0) {
                // End line & not EOF
                if (buf[i] == '\n') {
                    if (offset == fileSizeMark && i == bytesRead - 1) {
                        fileSizeMark = -1;
                    } else {
                        lines--;
                    }
                }
                i--;
            }
            if (lines == 0) {
                readLines(inputFile, outputStream, offset - bytesRead + i + 2);
                return;
            }
            offset -= bytesRead;
            if (offset - bufferSize >= 0) {
                seek = offset - bufferSize;
                len = bufferSize;
            } else {
                seek = 0;
                len = (int) offset;
            }
            inputFile.seek(seek);
            bytesRead = inputFile.read(buf, 0, len);
        }
        readLines(inputFile, outputStream, 0);
    }

    private static void illegalArgumentExceptionCheck(int bufferSize, int numLines) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize < 1");
        }
        if (numLines < 1) {
            throw new IllegalArgumentException("numLines < 1");
        }
    }
}
