package net.jpountz.lz4;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static net.jpountz.lz4.Instances.COMPRESSORS;
import static net.jpountz.lz4.Instances.UNCOMPRESSORS;
import static net.jpountz.lz4.Instances.UNCOMPRESSORS2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;

@RunWith(RandomizedRunner.class)
public class LZ4Test extends RandomizedTest {

  private static byte[] getCompressedWorstCase(byte[] uncompressed) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int len = uncompressed.length;
    if (len >= LZ4Utils.RUN_MASK) {
      baos.write(LZ4Utils.RUN_MASK << LZ4Utils.ML_BITS);
      len -= LZ4Utils.RUN_MASK;
    }
    while (len >= 255) {
      baos.write(255);
      len -= 255;
    }
    baos.write(len);
    try {
      baos.write(uncompressed);
    } catch (IOException e) {
      throw new AssertionError();
    }
    return baos.toByteArray();
  }

  private static byte[] randomArray(int len, int max) {
    byte[] result = new byte[len];
    for (int i = 0; i < result.length; ++i) {
      result[i] = (byte) randomInt(max);
    }
    return result;
  }

  public void testEmpty(CompressionCodec compressionCodec) {
    final byte[] data = new byte[0];
    final int maxCompressedLength = compressionCodec.maxCompressedLength(0);
    final byte[] compressed = new byte[maxCompressedLength];
    final int compressedLength = compressionCodec.compress(data, 0, 0, compressed, 0);
    assertTrue(compressedLength > 0);
    assertTrue(compressedLength <= maxCompressedLength);
    assertEquals(0, compressionCodec.uncompress(compressed, 0, compressedLength, new byte[3], 1));
  }

  @Test
  public void testEmpty() {
    for (LZ4Compressor compressor : COMPRESSORS) {
      for (LZ4Uncompressor uncompressor : UNCOMPRESSORS) {
        testEmpty(new LengthLZ4(compressor, uncompressor));
      }
    }
    for (LZ4Compressor compressor : COMPRESSORS) {
      for (LZ4UnknownSizeUncompressor uncompressor : UNCOMPRESSORS2) {
        testEmpty(new LengthBitsLZ4(compressor, uncompressor));
      }
    }
  }

  public void testCompress(LZ4Compressor compressor, LZ4Uncompressor uncompressor) {
    testCompress(new LengthLZ4(compressor, uncompressor));
  }

  public void testCompress(LZ4Compressor compressor, LZ4UnknownSizeUncompressor uncompressor) {
    testCompress(new LengthBitsLZ4(compressor, uncompressor));
  }

  public void testCompress(CompressionCodec compressionCodec) {
    final int max = randomBoolean()
        ? randomInt(3)
        : randomInt(256);
    final int size = randomBoolean()
        ? randomBoolean() ? 19 : 20
            : randomBoolean() ? randomIntBetween(45, 64000) : randomIntBetween(200000, 1000000);
    final byte[] src = new byte[size];
    for (int i = 0; i < src.length; ++i) {
      // low values of max are more likely to produce repeated patterns...
      src[i] = (byte) randomInt(max);
    }
    final int maxCompressedLength = compressionCodec.maxCompressedLength(src.length - 15);
    byte[] compressed = new byte[maxCompressedLength + 10];
    final int compressedLength = compressionCodec.compress(src, 4, src.length - 15, compressed, 2);
    assertTrue(compressedLength <= maxCompressedLength);
    byte[] uncompressed = new byte[src.length];
    final int uncompressedLength = compressionCodec.uncompress(compressed, 2, compressedLength, uncompressed, 3);
    assertEquals(src.length - 15, uncompressedLength);
    final byte[] original = Arrays.copyOfRange(src, 4, 4 + src.length - 15);
    compressed = Arrays.copyOfRange(compressed, 2, compressedLength + 2);
    final byte[] restored = Arrays.copyOfRange(uncompressed, 3, 3 + uncompressedLength);
    assertArrayEquals(original, restored);
    assertArrayEquals(compressed, compressionCodec.compress(src, 4, src.length - 15));
    assertArrayEquals(original, compressionCodec.uncompress(compressed));
  }

  @Test
  @Repeat(iterations=5)
  public void testCompress() {
    final LZ4Compressor compressor = randomFrom(COMPRESSORS);
    testCompress(compressor, (LZ4Uncompressor) LZ4JNIUncompressor.INSTANCE);
  }

  @Test
  @Repeat(iterations=5)
  public void testUncompress() {
    final LZ4Uncompressor uncompressor = randomFrom(UNCOMPRESSORS);
    testCompress(LZ4JNICompressor.HIGH_COMPRESSION, uncompressor);
  }

  @Test
  @Repeat(iterations=5)
  public void testUncompressUnknownSize() {
    final LZ4UnknownSizeUncompressor uncompressor = randomFrom(UNCOMPRESSORS2);
    testCompress(LZ4JNICompressor.HIGH_COMPRESSION, uncompressor);
  }

  public void testUncompressWorstCase(LZ4Uncompressor uncompressor) {
    final int len = randomInt(100 * 1024);
    final int max = randomInt(256);
    byte[] uncompressed = randomArray(len, max);
    byte[] compressed = getCompressedWorstCase(uncompressed);
    byte[] restored = new byte[uncompressed.length];
    int cpLen = uncompressor.uncompress(compressed, 0, restored, 0, uncompressed.length);
    assertEquals(compressed.length, cpLen);
    assertArrayEquals(uncompressed, restored);
  }

  @Test
  public void testUncompressWorstCase() {
    for (LZ4Uncompressor uncompressor : UNCOMPRESSORS) {
      testUncompressWorstCase(uncompressor);
    }
  }

  public void testUncompressWorstCase(LZ4UnknownSizeUncompressor uncompressor) {
    final int len = randomInt(100 * 1024);
    final int max = randomInt(256);
    byte[] uncompressed = randomArray(len, max);
    byte[] compressed = getCompressedWorstCase(uncompressed);
    byte[] restored = new byte[uncompressed.length];
    int uncpLen = uncompressor.uncompressUnknownSize(compressed, 0, compressed.length, restored, 0);
    assertEquals(uncompressed.length, uncpLen);
    assertArrayEquals(uncompressed, restored);
  }

  @Test
  public void testUncompressUnknownSizeWorstCase() {
    for (LZ4UnknownSizeUncompressor uncompressor : UNCOMPRESSORS2) {
      testUncompressWorstCase(uncompressor);
    }
  }

  // doesn't work, should we fix it?
  //@Test(expected=LZ4Exception.class)
  @Repeat(iterations=5)
  public void testUncompressUnknownSizeUnderflow() {
    final LZ4UnknownSizeUncompressor uncompressor = randomFrom(UNCOMPRESSORS2);
    final int len = randomInt(100000);
    final int max = randomInt(256);
    final byte[] data = new byte[len];
    for (int i = 0; i < data.length; ++i) {
      data[i] = (byte) randomInt(max);
    }
    final int maxCompressedLength = LZ4JNICompressor.FAST.maxCompressedLength(len);
    final byte[] compressed = new byte[maxCompressedLength];
    final int compressedLength = LZ4JNICompressor.FAST.compress(data, 0, data.length, compressed, 0, compressed.length);
    uncompressor.uncompressUnknownSize(compressed, 0, compressedLength, new byte[data.length - 1], 0);
  }

  private static byte[] readResource(String resource) throws IOException {
    InputStream is = LZ4Test.class.getResourceAsStream(resource);
    if (is == null) {
      throw new IllegalStateException("Cannot find " + resource);
    }
    byte[] buf = new byte[4096];
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      while (true) {
        final int read = is.read(buf);
        if (read == -1) {
          break;
        }
        baos.write(buf, 0, read);
      }
    } finally {
      is.close();
    }
    return baos.toByteArray();
  }

  public void testRoundTrip(String resource,
      LZ4Compressor compressor,
      LZ4Uncompressor uncompressor,
      LZ4UnknownSizeUncompressor uncompressor2) throws IOException {
    final byte[] uncompressed = readResource(resource);
    final byte[] compressed = new byte[LZ4Utils.maxCompressedLength(uncompressed.length)];
    final int compressedLen = compressor.compress(
        uncompressed, 0, uncompressed.length,
        compressed, 0, compressed.length);

    final byte[] restored = new byte[uncompressed.length];
    assertEquals(compressedLen, uncompressor.uncompress(compressed, 0, restored, 0, uncompressed.length));
    assertArrayEquals(uncompressed, restored);

    Arrays.fill(restored, (byte) 0);
    uncompressor2.uncompressUnknownSize(compressed, 0, compressedLen, restored, 0);
    assertEquals(uncompressed.length, uncompressor2.uncompressUnknownSize(compressed, 0, compressedLen, restored, 0));
  }

  public void testRoundTrip(String resource, LZ4Factory lz4) throws IOException {
    for (LZ4Compressor compressor : Arrays.asList(
        lz4.fastCompressor(), lz4.highCompressor())) {
      testRoundTrip(resource, compressor, lz4.uncompressor(), lz4.unknwonSizeUncompressor());
    }
  }

  public void testRoundTrip(String resource) throws IOException {
    for (LZ4Factory lz4 : Arrays.asList(
        LZ4Factory.nativeInstance(),
        LZ4Factory.unsafeInstance(),
        LZ4Factory.safeInstance())) {
      testRoundTrip(resource, lz4);
    }
  }

  @Test
  public void testRoundtripGeo() throws IOException {
    testRoundTrip("/calgary/geo");
  }

  @Test
  public void testRoundtripBook1() throws IOException {
    testRoundTrip("/calgary/book1");
  }

  @Test
  public void testRoundtripPic() throws IOException {
    testRoundTrip("/calgary/pic");
  }

  @Test
  public void testNullMatchDec() {
    // 1 literal, 4 matchs with matchDec=0, 5 literals
    final byte[] invalid = new byte[] { 16, 42, 0, 0, 42, 42, 42, 42, 42 };
    for (LZ4Uncompressor uncompressor : UNCOMPRESSORS) {
      try {
        uncompressor.uncompress(invalid, 0, new byte[10], 0, 10);
        if (!uncompressor.toString().contains("JNI")) {
          assertTrue(uncompressor.toString(), false);
        }
      } catch (LZ4Exception e) {
        // OK
      }
    }
    for (LZ4UnknownSizeUncompressor uncompressor : UNCOMPRESSORS2) {
      try {
        uncompressor.uncompressUnknownSize(invalid, 0, invalid.length, new byte[10], 0);
        assertTrue(uncompressor.toString(), false);
      } catch (LZ4Exception e) {
        // OK
      }
    }
  }

  @Test
  public void testEndsWithMatch() {
    // 6 literals, 4 matchs
    final byte[] invalid = new byte[] { 96, 42, 43, 44, 45, 46, 47, 5, 0 };
    final int uncompressedLength = 10;

    for (LZ4Uncompressor uncompressor : UNCOMPRESSORS) {
      try {
        // it is invalid to end with a match, should be at least 5 literals
        uncompressor.uncompress(invalid, 0, new byte[uncompressedLength], 0, uncompressedLength);
        // TODO: disable the condition when the JNI instances are fixed
        if (!uncompressor.toString().contains("JNI")) {
          assertTrue(uncompressor.toString(), false);
        }
      } catch (LZ4Exception e) {
        // OK
      }
    }

    for (LZ4UnknownSizeUncompressor uncompressor : UNCOMPRESSORS2) {
      try {
        // it is invalid to end with a match, should be at least 5 literals
        uncompressor.uncompressUnknownSize(invalid, 0, invalid.length, new byte[20], 0);
        assertTrue(false);
      } catch (LZ4Exception e) {
        // OK
      }
    }
  }

  @Test
  public void testEndsWithLessThan5Literals() {
    // 6 literals, 4 matchs
    final byte[] invalidBase = new byte[] { 96, 42, 43, 44, 45, 46, 47, 5, 0 };

    for (int i = 1; i < 5; ++i) {
      final byte[] invalid = Arrays.copyOf(invalidBase, invalidBase.length + 1 + i);
      invalid[invalidBase.length] = (byte) (i << 4); // i literals at the end

      for (LZ4Uncompressor uncompressor : UNCOMPRESSORS) {
        try {
          // it is invalid to end with a match, should be at least 5 literals
          uncompressor.uncompress(invalid, 0, new byte[20], 0, 20);
          if (!uncompressor.toString().contains("JNI")) {
            assertTrue(uncompressor.toString(), false);
          }
        } catch (LZ4Exception e) {
          // OK
        }
      }

      for (LZ4UnknownSizeUncompressor uncompressor : UNCOMPRESSORS2) {
        try {
          // it is invalid to end with a match, should be at least 5 literals
          uncompressor.uncompressUnknownSize(invalid, 0, invalid.length, new byte[20], 0);
          assertTrue(false);
        } catch (LZ4Exception e) {
          // OK
        }
      }
    }
  }

}
