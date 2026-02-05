/*
 * Copyright 2024-2027 CIRPASS-2
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
package it.extared.dpp.validator.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MultiMap<K, V> extends HashMap<K, List<V>> {

    /**
     * Adds a single value to the underlying list associated to the provided key.
     *
     * @param key the key of the list
     * @param value the value to add.
     */
    public void add(K key, V value) {
        if (!containsKey(key)) {
            List<V> values = new ArrayList<>();
            put(key, values);
        }
        get(key).add(value);
    }
}
