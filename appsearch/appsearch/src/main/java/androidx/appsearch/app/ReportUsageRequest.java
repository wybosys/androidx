/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * A request to report usage of a document.
 *
 * <p>See {@link AppSearchSession#reportUsage} for a detailed description of usage reporting.
 *
 * @see AppSearchSession#reportUsage
 */
public final class ReportUsageRequest {
    private final String mNamespace;
    private final String mDocumentId;
    private final long mUsageTimestampMillis;

    ReportUsageRequest(
            @NonNull String namespace, @NonNull String documentId, long usageTimestampMillis) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mDocumentId = Preconditions.checkNotNull(documentId);
        mUsageTimestampMillis = usageTimestampMillis;
    }

    /** Returns the namespace of the document that was used. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the ID of document that was used. */
    @NonNull
    public String getDocumentId() {
        return mDocumentId;
    }

    /**
     * Returns the timestamp in milliseconds of the usage report (the time at which the document
     * was used).
     *
     * <p>The value is in the {@link System#currentTimeMillis} time base.
     */
    /*@exportToFramework:CurrentTimeMillisLong*/
    public long getUsageTimestampMillis() {
        return mUsageTimestampMillis;
    }

    /** Builder for {@link ReportUsageRequest} objects. */
    public static final class Builder {
        private final String mNamespace;
        // TODO(b/181887768): Make this final
        private String mDocumentId;
        private Long mUsageTimestampMillis;
        private boolean mBuilt = false;

        /** Creates a {@link ReportUsageRequest.Builder} instance. */
        public Builder(@NonNull String namespace, @NonNull String documentId) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mDocumentId = Preconditions.checkNotNull(documentId);
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        public Builder(@NonNull String namespace) {
            mNamespace = Preconditions.checkNotNull(namespace);
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        @NonNull
        public Builder setUri(@NonNull String uri) {
            mDocumentId = uri;
            return this;
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        @NonNull
        public ReportUsageRequest.Builder setUsageTimeMillis(
                /*@exportToFramework:CurrentTimeMillisLong*/ long usageTimestampMillis) {
            return setUsageTimestampMillis(usageTimestampMillis);
        }

        /**
         * Sets the timestamp in milliseconds of the usage report (the time at which the document
         * was used).
         *
         * <p>The value is in the {@link System#currentTimeMillis} time base.
         *
         * <p>If unset, this defaults to the current timestamp at the time that the
         * {@link ReportUsageRequest} is constructed.
         *
         * @throws IllegalStateException if the builder has already been used
         */
        @NonNull
        public ReportUsageRequest.Builder setUsageTimestampMillis(
                /*@exportToFramework:CurrentTimeMillisLong*/ long usageTimestampMillis) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mUsageTimestampMillis = usageTimestampMillis;
            return this;
        }

        /**
         * Builds a new {@link ReportUsageRequest}.
         *
         * @throws IllegalStateException if the builder has already been used
         */
        @NonNull
        public ReportUsageRequest build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            if (mUsageTimestampMillis == null) {
                mUsageTimestampMillis = System.currentTimeMillis();
            }
            mBuilt = true;
            return new ReportUsageRequest(mNamespace, mDocumentId, mUsageTimestampMillis);
        }
    }
}
