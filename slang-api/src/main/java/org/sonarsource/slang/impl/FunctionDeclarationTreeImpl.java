/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.slang.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;

public class FunctionDeclarationTreeImpl extends BaseTreeImpl implements FunctionDeclarationTree {

  private List<Tree> modifiers;
  private final boolean isConstructor;
  private final Tree returnType;
  private final IdentifierTree name;
  private final List<Tree> formalParameters;
  private final BlockTree body;
  private final List<Tree> children = new ArrayList<>();
  private final List<Tree> nativeChildren;

  public FunctionDeclarationTreeImpl(
    TreeMetaData metaData,
    List<Tree> modifiers,
    boolean isConstructor,
    @Nullable Tree returnType,
    @Nullable IdentifierTree name,
    List<Tree> formalParameters,
    @Nullable BlockTree body,
    List<Tree> nativeChildren) {
    super(metaData);

    this.modifiers = modifiers;
    this.isConstructor = isConstructor;
    this.returnType = returnType;
    this.name = name;
    this.formalParameters = formalParameters;
    this.body = body;
    this.nativeChildren = nativeChildren;

    this.children.addAll(modifiers);
    if (returnType != null) {
      this.children.add(returnType);
    }
    if (name != null) {
      this.children.add(name);
    }
    this.children.addAll(formalParameters);
    if (body != null) {
      this.children.add(body);
    }
    this.children.addAll(nativeChildren);
  }

  @Override
  public List<Tree> modifiers() {
    return Collections.unmodifiableList(modifiers);
  }

  public void setModifiers(List<Tree> modifiers) {
    this.modifiers = modifiers;
  }

  @Override
  public boolean isConstructor() {
    return isConstructor;
  }

  @CheckForNull
  @Override
  public Tree returnType() {
    return returnType;
  }

  @CheckForNull
  @Override
  public IdentifierTree name() {
    return name;
  }

  @Override
  public List<Tree> formalParameters() {
    return formalParameters;
  }

  @CheckForNull
  @Override
  public BlockTree body() {
    return body;
  }

  @Override
  public List<Tree> nativeChildren() {
    return nativeChildren;
  }

  @Override
  public TextRange rangeToHighlight() {
    if (name != null) {
      return name.metaData().textRange();
    }
    if (body == null) {
      return metaData().textRange();
    }
    TextRange bodyRange = body.metaData().textRange();
    List<TextRange> tokenRangesBeforeBody = metaData().tokens().stream()
      .map(Token::textRange)
      .filter(t -> t.start().compareTo(bodyRange.start()) < 0)
      .toList();
    if (tokenRangesBeforeBody.isEmpty()) {
      return bodyRange;
    }
    return TextRanges.merge(tokenRangesBeforeBody);
  }

  @Override
  public List<Tree> children() {
    return children;
  }
}
