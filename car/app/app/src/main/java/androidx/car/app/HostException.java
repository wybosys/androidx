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

package androidx.car.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Exceptions that happen on calls to the host. */
// Developers can catch this exception so keeping it.
public class HostException extends RuntimeException {
    /** @hide */
    @RestrictTo(LIBRARY)
    public HostException(@NonNull String message) {
        super(message);
    }

    /** @hide */
    @RestrictTo(LIBRARY)
    public HostException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }

    /** @hide */
    @RestrictTo(LIBRARY)
    public HostException(@NonNull Throwable cause) {
        super(cause);
    }
}
