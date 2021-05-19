/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement

object SyntacticEquivalence {
    private fun areEquivalent(first: Iterator<PsiElement>, second: Iterator<PsiElement>): Boolean {
        val firstIterator = first.iterator()
        val secondIterator = second.iterator()
        while (firstIterator.hasNext() && secondIterator.hasNext()) {
            if (!areEquivalent(firstIterator.next(), secondIterator.next())) {
                return false
            }
        }
        return !firstIterator.hasNext() && !secondIterator.hasNext();
    }

    fun areEquivalent(first: PsiElement, second: PsiElement): Boolean {
        if (first === second) {
            return true
        }
        if (first.javaClass != second.javaClass) {
            return false
        }
        val leftChildrenIterator = first.children.asSequence().filter { it is KtElement }.iterator()
        val rightChildrenIterator = second.children.asSequence().filter { it is KtElement }.iterator()
        if (!leftChildrenIterator.hasNext() && !rightChildrenIterator.hasNext()) {
            return first.text == second.text
        }
        return areEquivalent(leftChildrenIterator, rightChildrenIterator)
    }
}