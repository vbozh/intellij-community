// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.changes.ui

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.KeyedExtensionFactory
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlin.properties.Delegates

class ChangesGroupingSupport(project: Project, source: Any) {
  private val changeSupport = PropertyChangeSupport(source)
  private val groupingFactories = collectFactories(project)

  var groupingKey by Delegates.observable(NONE_GROUPING) { _, oldValue, newValue ->
    changeSupport.firePropertyChange(PROP_GROUPING_KEY, oldValue, newValue)
  }
  val grouping get() = groupingFactories.getByKey(groupingKey)!!
  val isNone get() = groupingKey == NONE_GROUPING

  fun isAvailable(groupingKey: String) = groupingFactories.getByKey(groupingKey) != null

  fun addPropertyChangeListener(listener: PropertyChangeListener) = changeSupport.addPropertyChangeListener(listener)
  fun removePropertyChangeListener(listener: PropertyChangeListener) = changeSupport.removePropertyChangeListener(listener)

  companion object {
    @JvmField val KEY = DataKey.create<ChangesGroupingSupport>("ChangesTree.GroupingSupport")!!
    const val PROP_GROUPING_KEY = "ChangesGroupingKey"
    const val DIRECTORY_GROUPING = "directory"
    const val NONE_GROUPING = "none"

    @JvmStatic
    fun collectFactories(project: Project) = object : KeyedExtensionFactory<ChangesGroupingPolicyFactory, String>(
      ChangesGroupingPolicyFactory::class.java, ChangesGroupingPolicyFactory.EP_NAME, project.picoContainer) {
      override fun getKey(key: String) = key
    }
  }
}