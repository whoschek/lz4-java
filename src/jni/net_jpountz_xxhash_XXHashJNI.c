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

#include "xxhash.h"
#include "net_jpountz_xxhash_XXHashJNI.h"

static jclass OutOfMemoryError;

/*
 * Class:     net_jpountz_xxhash_XXHashJNI
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_net_jpountz_xxhash_XXHashJNI_init
  (JNIEnv *env, jclass cls) {
  OutOfMemoryError = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
}

static void throw_OOM(JNIEnv *env) {
  (*env)->ThrowNew(env, OutOfMemoryError, "Out of memory");
}

/*
 * Class:     net_jpountz_xxhash_XXHashJNI
 * Method:    XXH_fast32
 * Signature: ([BIII)I
 */
JNIEXPORT jint JNICALL Java_net_jpountz_xxhash_XXHashJNI_XXH_1fast32
  (JNIEnv *env, jclass cls, jbyteArray buf, jint off, jint len, jint seed) {

  char* in = (char*) (*env)->GetPrimitiveArrayCritical(env, buf, 0);
  if (in == NULL) {
    throw_OOM(env);
    return 0;
  }

  return XXH_fast32(in + off, len, seed);
}

/*
 * Class:     net_jpountz_xxhash_XXHashJNI
 * Method:    XXH_strong32
 * Signature: ([BIII)I
 */
JNIEXPORT jint JNICALL Java_net_jpountz_xxhash_XXHashJNI_XXH_1strong32
  (JNIEnv *env, jclass cls, jbyteArray buf, jint off, jint len, jint seed) {

  char* in = (char*) (*env)->GetPrimitiveArrayCritical(env, buf, 0);
  if (in == NULL) {
    throw_OOM(env);
    return 0;
  }

  return XXH_strong32(in + off, len, seed);
}
