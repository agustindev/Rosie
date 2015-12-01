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

package com.karumi.rosie.repository;

import com.karumi.rosie.repository.datasource.Identifiable;
import com.karumi.rosie.repository.datasource.paginated.PaginatedCacheDataSource;
import com.karumi.rosie.repository.datasource.paginated.PaginatedReadableDataSource;
import com.karumi.rosie.repository.policy.ReadPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Paginated version of {@link RosieRepository}. It adds methods for retrieving paginated data
 */
public class PaginatedRosieRepository<K, V extends Identifiable<K>> extends RosieRepository<K, V> {

  private final Collection<PaginatedReadableDataSource<V>> paginatedReadableDataSources =
      new LinkedList<>();
  private final Collection<PaginatedCacheDataSource<K, V>> paginatedCacheDataSources =
      new LinkedList<>();

  @SafeVarargs
  protected final <R extends PaginatedReadableDataSource<V>> void addPaginatedReadableDataSources(
      R... readables) {
    this.paginatedReadableDataSources.addAll(Arrays.asList(readables));
  }

  @SafeVarargs
  protected final <R extends PaginatedCacheDataSource<K, V>> void addPaginatedCacheDataSources(
      R... caches) {
    this.paginatedCacheDataSources.addAll(Arrays.asList(caches));
  }

  public PaginatedCollection<V> getPage(int offset, int limit) throws Exception {
    return getPage(offset, limit, ReadPolicy.READ_ALL);
  }

  public PaginatedCollection<V> getPage(int offset, int limit, ReadPolicy policy) throws Exception {
    PaginatedCollection<V> values = null;

    if (policy.useCache()) {
      values = getPaginatedValuesFromCaches(offset, limit);
    }

    if (values == null && policy.useReadable()) {
      values = getPaginatedValuesFromReadables(offset, limit);
    }

    if (values != null) {
      populatePaginatedCaches(offset, limit, values);
    }

    return values;
  }

  protected PaginatedCollection<V> getPaginatedValuesFromCaches(int offset, int limit) {
    PaginatedCollection<V> values = null;

    for (PaginatedCacheDataSource<K, V> cacheDataSource : paginatedCacheDataSources) {
      values = cacheDataSource.getPage(offset, limit);

      if (values != null) {
        if (areValidValues(values, cacheDataSource)) {
          break;
        } else {
          cacheDataSource.deleteAll();
          values = null;
        }
      }
    }

    return values;
  }

  protected PaginatedCollection<V> getPaginatedValuesFromReadables(int offset, int limit) {
    PaginatedCollection<V> values = null;

    for (PaginatedReadableDataSource<V> readable : paginatedReadableDataSources) {
      values = readable.getPage(offset, limit);

      if (values != null) {
        break;
      }
    }

    return values;
  }

  protected void populatePaginatedCaches(int offset, int limit, PaginatedCollection<V> values) {
    for (PaginatedCacheDataSource<K, V> cacheDataSource : paginatedCacheDataSources) {
      cacheDataSource.addOrUpdatePage(offset, limit, values.getItems(), values.hasMore());
    }
  }

  private boolean areValidValues(PaginatedCollection<V> values,
      PaginatedCacheDataSource<K, V> cacheDataSource) {
    boolean areValidValues = false;
    for (V value : values.getItems()) {
      areValidValues |= cacheDataSource.isValid(value);
    }
    return areValidValues;
  }
}
