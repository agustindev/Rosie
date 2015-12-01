/*
 * The MIT License (MIT) Copyright (c) 2014 karumi Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to
  * do so, subject to the following conditions: The above copyright notice and this permission
  * notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE
  * IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
  * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
  * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.karumi.rosie.repository.datasource.paginated;

import com.karumi.rosie.repository.PaginatedCollection;
import com.karumi.rosie.repository.datasource.Identifiable;
import com.karumi.rosie.time.TimeProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class InMemoryPaginatedCacheDataSource<K, V extends Identifiable<K>>
    implements PaginatedCacheDataSource<K, V> {

  private final TimeProvider timeProvider;
  private final long ttlInMillis;
  private final List<V> items;

  private long lastItemsUpdate;
  private boolean hasMore;

  public InMemoryPaginatedCacheDataSource(TimeProvider timeProvider, long ttlInMillis) {
    this.timeProvider = timeProvider;
    this.ttlInMillis = ttlInMillis;
    this.items = new ArrayList<>();
  }

  @Override public PaginatedCollection<V> getPage(int offset, int limit) {
    List<V> result = new LinkedList<>();
    for (int i = offset; i < items.size() && i < offset + limit; i++) {
      V value = items.get(i);
      result.add(value);
    }
    PaginatedCollection<V> paginatedCollection = new PaginatedCollection<>(result);
    paginatedCollection.setOffset(offset);
    paginatedCollection.setLimit(limit);
    paginatedCollection.setHasMore(offset + limit < items.size() || this.hasMore);
    return paginatedCollection;
  }

  @Override
  public PaginatedCollection<V> addOrUpdatePage(int offset, int limit, Collection<V> items,
      boolean hasMore) {
    this.items.addAll(items);
    this.hasMore = hasMore;
    PaginatedCollection<V> paginatedCollection = new PaginatedCollection<>(items);
    paginatedCollection.setOffset(offset);
    paginatedCollection.setLimit(limit);
    paginatedCollection.setHasMore(hasMore);
    lastItemsUpdate = timeProvider.currentTimeMillis();
    return paginatedCollection;
  }

  @Override public void deleteAll() {
    items.clear();
    hasMore = false;
    lastItemsUpdate = 0;
  }

  @Override public boolean isValid(V value) {
    return timeProvider.currentTimeMillis() - lastItemsUpdate < ttlInMillis;
  }
}
