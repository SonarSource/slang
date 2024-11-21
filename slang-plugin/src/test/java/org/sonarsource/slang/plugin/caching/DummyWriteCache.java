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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;

public class DummyWriteCache implements WriteCache {
  public final Map<String, byte[]> persisted = new HashMap<>();
  ReadCache previousCache;

  @Override
  public void write(String key, InputStream data) {
    try {
      write(key, data.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(String key, byte[] data) {
    if (persisted.containsKey(key)) {
      throw new IllegalArgumentException(String.format("The cache already contains the key: %s", key));
    }
    persisted.put(key, data);
  }

  @Override
  public void copyFromPrevious(String key) {
    if (previousCache == null) {
      throw new IllegalStateException("The write cache needs to be bound with a ReadCache!");
    }
    write(key, previousCache.read(key));
  }

  public void bind(DummyReadCache previousCache) {
    this.previousCache = previousCache;
  }
}
