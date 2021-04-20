/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appsearch.app.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

public class AppSearchTestUtils {

    public static <K, V> AppSearchBatchResult<K, V> checkIsBatchResultSuccess(
            Future<AppSearchBatchResult<K, V>> future) throws Exception {
        AppSearchBatchResult<K, V> result = future.get();
        assertWithMessage("AppSearchBatchResult not successful: " + result)
                .that(result.isSuccess()).isTrue();
        return result;
    }

    public static List<GenericDocument> doGet(
            AppSearchSession session, String namespace, String... ids) throws Exception {
        AppSearchBatchResult<String, GenericDocument> result = checkIsBatchResultSuccess(
                session.getByDocumentId(
                        new GetByDocumentIdRequest.Builder(namespace).addIds(ids).build()));
        assertThat(result.getSuccesses()).hasSize(ids.length);
        assertThat(result.getFailures()).isEmpty();
        List<GenericDocument> list = new ArrayList<>(ids.length);
        for (String id : ids) {
            list.add(result.getSuccesses().get(id));
        }
        return list;
    }

    public static List<GenericDocument> doGet(
            AppSearchSession session, GetByDocumentIdRequest request) throws Exception {
        AppSearchBatchResult<String, GenericDocument> result = checkIsBatchResultSuccess(
                session.getByDocumentId(request));
        Set<String> ids = request.getIds();
        assertThat(result.getSuccesses()).hasSize(ids.size());
        assertThat(result.getFailures()).isEmpty();
        List<GenericDocument> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            list.add(result.getSuccesses().get(id));
        }
        return list;
    }

    public static List<GenericDocument> convertSearchResultsToDocuments(SearchResults searchResults)
            throws Exception {
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        List<GenericDocument> documents = new ArrayList<>(results.size());
        for (SearchResult result : results) {
            documents.add(result.getGenericDocument());
        }
        return documents;
    }

    public static List<SearchResult> retrieveAllSearchResults(SearchResults searchResults)
            throws Exception {
        List<SearchResult> page = searchResults.getNextPage().get();
        List<SearchResult> results = new ArrayList<>();
        while (!page.isEmpty()) {
            results.addAll(page);
            page = searchResults.getNextPage().get();
        }
        return results;
    }
}
