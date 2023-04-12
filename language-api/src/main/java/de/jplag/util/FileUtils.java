package de.jplag.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Encapsulates various interactions with files to prevent issues with file encodings.
 */
public class FileUtils {
    private static final Charset defaultOutputCharset = StandardCharsets.UTF_8;

    /**
     * Opens a file reader, guessing the charset from the content. Also, if the file is encoded in a UTF* encoding and a bom
     * exists, it is removed from the reader.
     * @param file The file to open for read
     * @return The reader, configured with the best matching charset
     * @throws IOException If the file does not exist for is not readable
     */
    public static BufferedReader openFileReader(File file) throws IOException {
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        Charset charset = detectCharset(stream);
        BufferedReader reader = new BufferedReader(new FileReader(file, charset));
        removeBom(reader, charset);
        return reader;
    }

    /**
     * Reads the contents of a file into a single string.
     * @param file The file to read
     * @return The files content as a string
     * @throws IOException If an IO error occurs
     * @see FileUtils#openFileReader(File)
     */
    public static String readFileContent(File file) throws IOException {
        try (BufferedReader reader = openFileReader(file)) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    /**
     * Removes the byte order mark from the beginning of the stream, if it exists and the charset is a UTF* charset. For
     * details see: <a href="https://en.wikipedia.org/wiki/Byte_order_mark">Wikipedia</a>
     * @param reader The reader to remove the bom from
     * @throws IOException If an IO error occurs.
     */
    private static void removeBom(BufferedReader reader, Charset charset) throws IOException {
        if (charset.name().toUpperCase().startsWith("UTF")) {
            reader.mark(10);
            if (reader.read() != '\uFEFF') {
                reader.reset();
            }
        }
    }

    /**
     * Detects the charset of a file. Prefer using {@link #openFileReader(File)} or {@link #readFileContent(File)} if you
     * are only interested in the content.
     * @param file The file to detect
     * @return The most probable charset
     * @throws IOException If an IO error occurs
     */
    public static Charset detectCharset(File file) throws IOException {
        try (InputStream stream = new BufferedInputStream(new FileInputStream((file)))) {
            return detectCharset(stream);
        }
    }

    /**
     * Detects the most probable charset over the whole set of files.
     * @param files The files to check
     * @return The most probable charset
     */
    public static Charset detectCharsetFromMultiple(Collection<File> files) {
        Map<String, List<Integer>> charsetValues = new HashMap<>();

        files.stream().map(it -> {
            try (InputStream stream = new BufferedInputStream(new FileInputStream(it))) {
                return detectAllCharsets(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).forEach(matches -> {
            Set<String> remaining = new HashSet<>(Set.of(CharsetDetector.getAllDetectableCharsets()));
            for (CharsetMatch match : matches) {
                charsetValues.putIfAbsent(match.getName(), new ArrayList<>());
                charsetValues.get(match.getName()).add(match.getConfidence());
                remaining.remove(match.getName());
            }
            remaining.forEach(it -> {
                charsetValues.putIfAbsent(it, new ArrayList<>());
                charsetValues.get(it).add(0);
            });
        });

        AtomicReference<Charset> mostProbable = new AtomicReference<>(StandardCharsets.UTF_8);
        AtomicReference<Double> mostProbableConfidence = new AtomicReference<>((double) 0);
        charsetValues.forEach((charset, confidenceValues) -> {
            double average = confidenceValues.stream().mapToInt(it -> it).average().orElse(0);
            if (average > mostProbableConfidence.get()) {
                mostProbable.set(Charset.forName(charset));
                mostProbableConfidence.set(average);
            }
        });

        return mostProbable.get();
    }

    private static Charset detectCharset(InputStream stream) throws IOException {
        CharsetDetector charsetDetector = new CharsetDetector();

        charsetDetector.setText(stream);

        CharsetMatch match = charsetDetector.detect();
        return Charset.forName(match.getName());
    }

    private static CharsetMatch[] detectAllCharsets(InputStream stream) throws IOException {
        CharsetDetector charsetDetector = new CharsetDetector();

        charsetDetector.setText(stream);

        return charsetDetector.detectAll();
    }

    /**
     * Opens a file writer, using the default charset for JPlag
     * @param file The file to write
     * @return The file writer, configured with the default charset
     * @throws IOException If the file does not exist or is not writable
     */
    public static Writer openFileWriter(File file) throws IOException {
        return new BufferedWriter(new FileWriter(file, defaultOutputCharset));
    }
}
