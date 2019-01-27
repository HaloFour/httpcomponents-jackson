/*
 * Copyright 2018, OK2 Consulting Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ok2c.hc5.json.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;

abstract class AbstractJsonEntityProducer implements AsyncEntityProducer {

    private enum State { ACTIVE, FLUSHING, END_STREAM }

    private final InternalBuffer buffer;

    private volatile State state;

    AbstractJsonEntityProducer(int initSize) {
        this.buffer = new InternalBuffer(initSize);
        this.state = State.ACTIVE;
    }

    abstract void generateJson(OutputStream outputStream) throws IOException;

    @Override
    public final long getContentLength() {
        return -1;
    }

    @Override
    public final Set<String> getTrailerNames() {
        return null;
    }

    @Override
    public final String getContentType() {
        return ContentType.APPLICATION_JSON.toString();
    }

    @Override
    public final String getContentEncoding() {
        return null;
    }

    @Override
    public final boolean isRepeatable() {
        return true;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public final int available() {
        return buffer.length();
    }

    @Override
    public final void produce(final DataStreamChannel channel) throws IOException {
        if (state == State.ACTIVE) {
            generateJson(new OutputStream() {

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    buffer.write(b, off, len);
                }

                @Override
                public void write(int b) throws IOException {
                    buffer.write(b);
                }

                @Override
                public void close() throws IOException {
                    state = State.FLUSHING;
                }

            });
        }
        if (state.compareTo(State.END_STREAM) < 0 && buffer.hasData()) {
            channel.write(buffer.getByteBuffer());
        }
        if (state == State.FLUSHING && !buffer.hasData()) {
            channel.endStream(null);
            state = State.END_STREAM;
        }
    }

    @Override
    public void releaseResources() {
        buffer.clear();
        state = State.ACTIVE;
    }

}
