// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model

import com.intellij.lang.javascript.psi.JSType
import com.intellij.psi.PsiElement

interface VueContainer : VueEntitiesContainer {
  val data: List<VueDataProperty>
  val computed: List<VueComputedProperty>
  val methods: List<VueMethod>
  val props: List<VueInputProperty>
  val emits: List<VueEmitCall>
  val slots: List<VueSlot>

  val template: PsiElement?
  val element: String?
  val extends: List<VueContainer>
  val model: VueModelDirectiveProperties
}

class VueModelDirectiveProperties(
  val prop: String = DEFAULT_PROP,
  val event: String = DEFAULT_EVENT
) {
  companion object {
    const val DEFAULT_PROP = "value"
    const val DEFAULT_EVENT = "input"
  }
}

interface VueSlot : VueNamedSymbol {
  val scope: JSType? get() = null
}

interface VueEmitCall : VueNamedSymbol {
  val eventJSType: JSType? get() = null
}

interface VueNamedSymbol {
  val name: String
  val source: PsiElement? get() = null
}

interface VueProperty : VueNamedSymbol {
  val jsType: JSType? get() = null
}

interface VueInputProperty : VueProperty {
  val required: Boolean
}

interface VueDataProperty : VueProperty

interface VueComputedProperty : VueProperty

interface VueMethod : VueNamedSymbol
