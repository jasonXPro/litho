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

package com.facebook.litho.sections.widget;

import com.facebook.litho.widget.StaggeredGridLayoutInfo;

/**
 * A Factory used to create {@link StaggeredGridLayoutInfo}s in {@link
 * StaggeredGridRecyclerConfiguration}.
 */
public interface StaggeredGridLayoutInfoFactory {
  StaggeredGridLayoutInfo createStaggeredGridLayoutInfo(
      int spanCount, int orientation, boolean reverseLayout, int gapStrategy);
}
