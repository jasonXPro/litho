/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.extensions;

import android.graphics.Rect;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderCoreExtensionHost;
import com.facebook.rendercore.RenderCoreSystrace;
import java.util.List;
import java.util.Stack;

/**
 * The base class for all RenderCore Extensions.
 *
 * @param <Input> the state the extension operates on.
 */
public class RenderCoreExtension<Input, State> {

  /**
   * The extension can optionally return a {@link LayoutResultVisitor} for every layout pass which
   * will visit every {@link LayoutResult}. The visitor should be functional and immutable.
   *
   * @return a {@link LayoutResultVisitor}.
   */
  public @Nullable LayoutResultVisitor<? extends Input> getLayoutVisitor() {
    return null;
  }

  /**
   * The extension can optionally return a {@link MountExtension} which can be used to augment the
   * RenderCore's mounting phase. The {@link #< Input >} collected in the latest layout pass will be
   * passed to the extension before mount.
   *
   * @return a {@link MountExtension}.
   */
  public @Nullable MountExtension<? extends Input, State> getMountExtension() {
    return null;
  }

  /**
   * Should return a new {@link #< Input >} to which the {@link LayoutResultVisitor} can write into.
   *
   * @return A new {@link #< Input >} for {@link LayoutResultVisitor} to write into.
   */
  public @Nullable Input createInput() {
    return null;
  }

  /**
   * Calls {@link MountExtension#beforeMount(ExtensionState, Object, Rect)} for each {@link
   * RenderCoreExtension} that has a mount phase.
   *
   * @param host The {@link Host} of the extensions
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public static void beforeMount(
      final MountDelegateTarget mountDelegateTarget,
      final Host host,
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> results) {
    if (results != null) {
      final Rect rect = new Rect();
      host.getLocalVisibleRect(rect);
      for (Pair<RenderCoreExtension<?, ?>, Object> entry : results) {
        final Object input = entry.second;
        final MountExtension extension = entry.first.getMountExtension();
        if (extension != null) {
          extension.beforeMount(mountDelegateTarget.getExtensionState(extension), input, rect);
        }
      }
    }
  }

  /**
   * Calls {@link MountExtension#afterMount(ExtensionState)} for each {@link RenderCoreExtension}
   * that has a mount phase.
   *
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public static void afterMount(
      final MountDelegateTarget mountDelegateTarget,
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> results) {
    if (results != null) {
      for (Pair<RenderCoreExtension<?, ?>, Object> entry : results) {
        final MountExtension<?, ?> extension = entry.first.getMountExtension();
        if (extension != null) {
          extension.afterMount(mountDelegateTarget.getExtensionState(extension));
        }
      }
    }
  }

  /**
   * Calls {@link MountExtension#onVisibleBoundsChanged(ExtensionState, Rect)} for each {@link
   * RenderCoreExtension} that has a mount phase.
   *
   * @param host The {@link Host} of the extensions
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public static void notifyVisibleBoundsChanged(
      final MountDelegateTarget mountDelegateTarget,
      final Host host,
      @Nullable final List<Pair<RenderCoreExtension<?, ?>, Object>> results) {
    if (results != null) {
      final Rect rect = new Rect();
      host.getLocalVisibleRect(rect);
      for (Pair<RenderCoreExtension<?, ?>, Object> e : results) {
        final MountExtension<?, ?> extension = e.first.getMountExtension();
        if (extension != null) {
          final ExtensionState state = mountDelegateTarget.getExtensionState(extension);
          if (state != null) {
            extension.onVisibleBoundsChanged(state, rect);
          }
        }
      }
    }
  }

  /** returns {@code false} iff the results have the same {@link RenderCoreExtension}s. */
  public static boolean shouldUpdate(
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> current,
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> next) {

    if (current == next) {
      return false;
    }

    if (current == null || next == null) {
      return true;
    }

    if (current.size() != next.size()) {
      return true;
    }

    for (int i = 0, size = current.size(); i < size; i++) {
      if (!current.get(i).first.equals(next.get(i).first)) {
        return true;
      }
    }

    return false;
  }

  public static void recursivelyNotifyVisibleBoundsChanged(final @Nullable Object content) {
    RenderCoreSystrace.beginSection("recursivelyNotifyVisibleBoundsChanged");

    if (content != null) {
      final Stack<Object> contentStack = new Stack<>();
      contentStack.add(content);

      while (!contentStack.isEmpty()) {
        final Object currentContent = contentStack.pop();

        if (currentContent instanceof RenderCoreExtensionHost) {
          ((RenderCoreExtensionHost) currentContent).notifyVisibleBoundsChanged();
        } else if (currentContent instanceof ViewGroup) {
          final ViewGroup currentViewGroup = (ViewGroup) currentContent;
          for (int i = currentViewGroup.getChildCount() - 1; i >= 0; i--) {
            contentStack.push(currentViewGroup.getChildAt(i));
          }
        }
      }
    }

    RenderCoreSystrace.endSection();
  }
}
