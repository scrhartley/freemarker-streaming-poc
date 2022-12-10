package simon.example.streaming.freemarker.custom;

import java.io.IOException;
import java.io.Writer;

public class ExceptionAwareWriter extends Writer {

    private final Writer mainWriter;
    private final Writer exceptionWriter;

    public ExceptionAwareWriter(Writer mainWriter, Writer exceptionWriter) {
        this.mainWriter = mainWriter;
        this.exceptionWriter = exceptionWriter;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        mainWriter.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        mainWriter.flush();
    }

    @Override
    public void close() throws IOException {
        mainWriter.close();
    }

    public Writer getExceptionWriter() {
        return exceptionWriter;
    }

}
