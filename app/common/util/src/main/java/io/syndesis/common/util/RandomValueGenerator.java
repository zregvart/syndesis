/*
 * Copyright (C) 2016 Red Hat, Inc.
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
package io.syndesis.common.util;

import java.security.SecureRandom;

/**
 * Generates strings values according to a generator string.
 * Examples of generator string: "alphanum", "alphanum:50".
 */
public final class RandomValueGenerator {

    private static final int ALPHANUM_DEFAULT_LENGTH = 40;
    private static final String ALPHANUM_DOMAIN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String ALPHANUM_SCHEME = "alphanum";

    private static final SecureRandom RANDOMIZER = new SecureRandom();

    private RandomValueGenerator() {
    }

    public static String generate(final String generator) {
        if (generator == null) {
            throw new IllegalArgumentException("Generator cannot be null");
        }

        final int split = generator.indexOf(':');
        if (split < 0) {
            return generate(generator, null);
        }

        return generate(generator.substring(0, split), generator.substring(split + 1));
    }

    private static String generate(final String scheme, final String remaining) {
        if (ALPHANUM_SCHEME.equals(scheme)) {
            return generateAlphanum(remaining);
        }

        throw new IllegalArgumentException("Unsupported generator scheme: " + scheme);
    }

    private static String generateAlphanum(final String remaining) {
        int length = ALPHANUM_DEFAULT_LENGTH;
        if (!Strings.isEmptyOrBlank(remaining)) {
            try {
                length = Integer.parseInt(remaining.trim());
            } catch (final NumberFormatException ex) {
                throw new IllegalArgumentException("Unexpected string after the " + ALPHANUM_SCHEME + " scheme: expected length", ex);
            }
        }

        if (length < 0) {
            throw new IllegalArgumentException("Cannot generate a string of negative length");
        }

        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < length; i++) {
            final int offset = RANDOMIZER.nextInt(ALPHANUM_DOMAIN.length());
            res.append(ALPHANUM_DOMAIN.charAt(offset));
        }

        return res.toString();
    }

}
