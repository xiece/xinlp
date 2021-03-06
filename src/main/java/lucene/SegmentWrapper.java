package lucene;

import lombok.Data;
import segment.Segment;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

@Data
public class SegmentWrapper {
    /**
     * 输入
     */
    private Reader input;
    /**
     * 分词器
     */
    private Segment segment;
    /**
     * 分词结果
     */
    private Iterator<Atom> iterator;
    /**
     * term的偏移量，由于wrapper是按行读取的，必须对term.offset做一个校正
     */
    int offset;
    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 512;
    /**
     * 缓冲区
     */
    private char[] buffer = new char[BUFFER_SIZE];
    /**
     * 缓冲区未处理的下标
     */
    private int remainSize = 0;

    public SegmentWrapper(Reader reader, Segment segment) {
        this.input = reader;
        this.segment = segment;
    }

    /**
     * 重置分词器
     *
     * @param reader
     */
    public void reset(Reader reader) {
        input = reader;
        offset = 0;
        iterator = null;
    }

    public Atom next() throws IOException {
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        System.out.println("------------");
        String line = readLine();
        if (line == null) {
            System.out.println("-******");
            return null;
        }
        List<Atom> atomList = segment.seg(line);
        if (atomList.size() == 0) {
            return null;
        }
        offset += line.length();
        iterator = atomList.iterator();
        return iterator.next();
    }

    private String readLine() throws IOException {
        int offset = 0;
        int length = BUFFER_SIZE;
        if (remainSize > 0) {
            offset = remainSize;
            length -= remainSize;
        }
        int n = input.read(buffer, offset, length);
        if (n < 0) {
            if (remainSize != 0) {
                String lastLine = new String(buffer, 0, remainSize);
                remainSize = 0;
                return lastLine;
            }
            return null;
        }
        n += offset;

        int eos = lastIndexOfEos(buffer, n);
        String line = new String(buffer, 0, eos);
        remainSize = n - eos;
        System.out.println("remainSize=" + remainSize);
        System.arraycopy(buffer, eos, buffer, 0, remainSize);
        return line;
    }

    private int lastIndexOfEos(char[] buffer, int length) {
        for (int i = length - 1; i > 0; i--) {
            if (buffer[i] == '\n' || CharType.get(buffer[i]) == CharType.CT_DELIMITER) {
                return i + 1;
            }
        }
        return length;
    }
}
