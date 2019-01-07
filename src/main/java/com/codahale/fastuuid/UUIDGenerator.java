/*
 * Copyright © 2018 Coda Hale (coda.hale@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codahale.fastuuid;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * A class which generates {@link UUID} instances using SipHash-2-4 in a fast-key-erasure CSPRNG.
 *
 * <p>For each UUID, it uses SipHash-2-4 to hash four single-byte values selected for their high
 * Hamming distances from each other. The first two results are used to re-key the hash; the second
 * two are used to produce the UUID.
 *
 * <p>This design allows for very fast UUID generation (~50ns/UUID) as well as forward-secrecy (i.e.
 * a compromised generator reveals no information about previously-generated UUIDs).
 *
 * <p>To provide backward-secrecy (i.e. a compromised generator reveals no information about UUIDs
 * which will be generated), the generator should be periodically re-seeded.
 */
public class UUIDGenerator {
  // four bytes selected for their relatively high Hamming distances
  private static final byte A = 0b00000110;
  private static final byte B = 0b01111111;
  private static final byte C = (byte) 0b10111000;
  private static final byte D = (byte) 0b11000001;

  // underlying PRNG
  private final SecureRandom random;

  // SipHash state
  private long v0, v1, v2, v3;

  /**
   * Creates a new {@link UUIDGenerator} seeded from the given PRNG.
   *
   * @param random a PRNG to use for a seed
   */
  public UUIDGenerator(SecureRandom random) {
    this.random = random;
    reseed();
  }

  /** Re-seeds the {@link UUIDGenerator}. */
  public void reseed() {
    reseed(random.nextLong(), random.nextLong());
  }

  private void reseed(long k0, long k1) {
    // SipHash magic constants
    this.v0 = k0 ^ 0x736F6D6570736575L;
    this.v1 = k1 ^ 0x646F72616E646F6DL;
    this.v2 = k0 ^ 0x6C7967656E657261L;
    this.v3 = k1 ^ 0x7465646279746573L;
  }

  /**
   * Generates a random {@link UUID}.
   *
   * @return a random {@link UUID}
   */
  public UUID generate() {
    final long k0 = sipHash24(v0, v1, v2, v3, A);
    final long k1 = sipHash24(v0, v1, v2, v3, B);
    final long msb = (sipHash24(v0, v1, v2, v3, C) & ~0xF000L) | 0x4000L;
    final long lsb = ((sipHash24(v0, v1, v2, v3, D) << 2) >>> 2) | 0x8000000000000000L;
    reseed(k0, k1);
    return new UUID(msb, lsb);
  }

  // a very slimmed-down version of SipHash-2-4 which operates on a single byte
  @SuppressWarnings("Duplicates")
  private static long sipHash24(long v0, long v1, long v2, long v3, byte data) {
    final long m = (data & 0xFFL) | 0x100000000000000L; // simplify the masking

    v3 ^= m;
    for (int i = 0; i < 2; i++) { // put the 2 in SipHash-2-4
      v0 += v1;
      v2 += v3;
      v1 = Long.rotateLeft(v1, 13);
      v3 = Long.rotateLeft(v3, 16);

      v1 ^= v0;
      v3 ^= v2;
      v0 = Long.rotateLeft(v0, 32);

      v2 += v1;
      v0 += v3;
      v1 = Long.rotateLeft(v1, 17);
      v3 = Long.rotateLeft(v3, 21);

      v1 ^= v2;
      v3 ^= v0;
      v2 = Long.rotateLeft(v2, 32);
    }
    v0 ^= m;

    v2 ^= 0xFF;
    for (int i = 0; i < 4; i++) { // put the 4 in SipHash-2-4
      v0 += v1;
      v2 += v3;
      v1 = Long.rotateLeft(v1, 13);
      v3 = Long.rotateLeft(v3, 16);

      v1 ^= v0;
      v3 ^= v2;
      v0 = Long.rotateLeft(v0, 32);

      v2 += v1;
      v0 += v3;
      v1 = Long.rotateLeft(v1, 17);
      v3 = Long.rotateLeft(v3, 21);

      v1 ^= v2;
      v3 ^= v0;
      v2 = Long.rotateLeft(v2, 32);
    }
    return v0 ^ v1 ^ v2 ^ v3;
  }
}
