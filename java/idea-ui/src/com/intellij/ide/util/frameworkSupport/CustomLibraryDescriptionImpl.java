/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.util.frameworkSupport;

import com.intellij.framework.library.DownloadableLibraryDescription;
import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryFilter;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public class CustomLibraryDescriptionImpl extends CustomLibraryDescriptionBase {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.util.frameworkSupport.CustomLibraryDescriptionImpl");
  private final DownloadableLibraryType myLibraryType;

  public CustomLibraryDescriptionImpl(@NotNull DownloadableLibraryType downloadableLibraryType) {
    super(downloadableLibraryType.getLibraryCategoryName());
    myLibraryType = downloadableLibraryType;
  }

  @Override
  public DownloadableLibraryDescription getDownloadableDescription() {
    return myLibraryType.getLibraryDescription();
  }

  @Override
  public DownloadableLibraryType getDownloadableLibraryType() {
    return myLibraryType;
  }

  @NotNull
  @Override
  public LibraryFilter getSuitableLibraryFilter() {
    return new LibraryFilter() {
      @Override
      public boolean isSuitableLibrary(@NotNull List<VirtualFile> classesRoots,
                                       @Nullable LibraryType<?> type) {
        return myLibraryType.equals(type);
      }
    };
  }

  public static CustomLibraryDescriptionImpl createDescription(Class<? extends DownloadableLibraryType> typeClass) {
    final DownloadableLibraryType libraryType = LibraryType.EP_NAME.findExtension(typeClass);
    LOG.assertTrue(libraryType != null, typeClass);
    return new CustomLibraryDescriptionImpl(libraryType);
  }
}
