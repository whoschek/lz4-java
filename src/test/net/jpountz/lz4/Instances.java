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


enum Instances {
  ;

  static LZ4Compressor[] COMPRESSORS = new LZ4Compressor[] {
    LZ4Factory.nativeInstance().fastCompressor(),
    LZ4Factory.nativeInstance().highCompressor(),
    LZ4Factory.unsafeInstance().fastCompressor(),
    LZ4Factory.unsafeInstance().highCompressor(),
    LZ4Factory.safeInstance().fastCompressor(),
    LZ4Factory.safeInstance().highCompressor()
  };

  static LZ4Uncompressor[] UNCOMPRESSORS = new LZ4Uncompressor[] {
    LZ4Factory.nativeInstance().uncompressor(),
    LZ4Factory.unsafeInstance().uncompressor(),
    LZ4Factory.safeInstance().uncompressor()
  };

  static LZ4UnknownSizeUncompressor[] UNCOMPRESSORS2 = new LZ4UnknownSizeUncompressor[] {
    LZ4Factory.nativeInstance().unknwonSizeUncompressor(),
    LZ4Factory.unsafeInstance().unknwonSizeUncompressor(),
    LZ4Factory.safeInstance().unknwonSizeUncompressor()
  };

}
