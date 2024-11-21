/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.plugin.caching;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.sensor.cache.ReadCache;

public class DummyReadCache implements ReadCache {
  public final Map<String, byte[]> persisted = new HashMap<>();

  @Override
  public InputStream read(String key) {
    if (!persisted.containsKey(key)) {
      throw new IllegalArgumentException(String.format("Cache does not contain key %s", key));
    }
    return new ByteArrayInputStream(persisted.get(key));
  }

  @Override
  public boolean contains(String key) {
    return persisted.containsKey(key);
  }
}
