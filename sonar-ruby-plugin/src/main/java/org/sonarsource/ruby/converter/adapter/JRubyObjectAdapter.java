/*
 * SonarSource SLang
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.ruby.converter.adapter;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class JRubyObjectAdapter<T extends IRubyObject> {

  protected final Ruby runtime;
  protected final T underlyingRubyObject;

  protected JRubyObjectAdapter(Ruby runtime, T underlyingRubyObject) {
    this.runtime = runtime;
    this.underlyingRubyObject = underlyingRubyObject;
  }

  protected <U> U getFromUnderlying(String attribute, Class<U> clazz) {
    return (U) JavaEmbedUtils.invokeMethod(runtime, underlyingRubyObject, attribute, null, clazz);
  }

  public boolean isNull() {
    return underlyingRubyObject == null;
  }

}
