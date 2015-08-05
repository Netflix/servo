/*
 * Copyright 2014 Netflix, Inc.
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

package com.netflix.servo.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Invoke a callable aborting after a certain timout.
 */
public final class TimeLimiter {
  private final ExecutorService executor;

  /**
   * Create a new TimeLimiter that will use the provided executor to invoke the Callable.
   *
   * @param executor {@link ExecutorService} used to invoke the callable
   */
  public TimeLimiter(ExecutorService executor) {
    this.executor = Preconditions.checkNotNull(executor, "executor");
  }

  /**
   * Invokes a specified Callable, timing out after the specified time limit.
   * If the target method call finished before the limit is reached, the return
   * value or exception is propagated to the caller exactly as-is.  If, on the
   * other hand, the time limit is reached, we attempt to abort the call to the
   * Callable and throw an exception.
   */
  public <T> T callWithTimeout(Callable<T> callable, long duration, TimeUnit unit)
      throws Exception {
    Future<T> future = executor.submit(callable);
    try {
      return future.get(duration, unit);
    } catch (InterruptedException e) {
      future.cancel(true);
      throw e;
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause == null) {
        cause = e;
      }
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw e;
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new UncheckedTimeoutException(e);
    }
  }

  /**
   * Unchecked version of {@link java.util.concurrent.TimeoutException}.
   */
  public static class UncheckedTimeoutException extends RuntimeException {
    /**
     * Create a new instance with a given cause.
     */
    public UncheckedTimeoutException(Exception cause) {
      super(cause);
    }
  }
}
