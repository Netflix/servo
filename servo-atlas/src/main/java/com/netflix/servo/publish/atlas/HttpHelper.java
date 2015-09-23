/**
 * Copyright 2015 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish.atlas;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.netflix.servo.util.Throwables;
import iep.com.netflix.iep.http.RxHttp;
import iep.io.reactivex.netty.protocol.http.client.HttpClientRequest;
import iep.io.reactivex.netty.protocol.http.client.HttpClientResponse;
import iep.io.reactivex.netty.protocol.http.client.HttpResponseHeaders;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to make http requests using rxhttp. For internal use of servo only.
 */
@Singleton
public final class HttpHelper {
  private static final JsonFactory SMILE_FACTORY = new SmileFactory();
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHelper.class);
  private static final String SMILE_CONTENT_TYPE = "application/x-jackson-smile";

  private final RxHttp rxHttp;

  /**
   * An HTTP Response. For internal use of servo only.
   */
  public static class Response {
    private int status;
    private byte[] body;
    private HttpResponseHeaders headers;

    /**
     * Get the HTTP status code.
     */
    public int getStatus() {
      return status;
    }

    /**
     * Get the body of the response as a byte array.
     */
    public byte[] getBody() {
      return Arrays.copyOf(body, body.length);
    }

    /**
     * Get the rxnetty {@link HttpResponseHeaders} for this response.
     */
    public HttpResponseHeaders getHeaders() {
      return headers;
    }
  }

  /**
   * Create a new HttpHelper using the given {@link RxHttp} instance.
   */
  @Inject
  public HttpHelper(RxHttp rxHttp) {
    this.rxHttp = rxHttp;
  }

  /**
   * Get the underlying {@link RxHttp} instance.
   */
  public RxHttp getRxHttp() {
    return rxHttp;
  }

  /**
   * POST to the given URI the passed {@link JsonPayload}.
   */
  public Observable<HttpClientResponse<ByteBuf>>
  postSmile(String uriStr, JsonPayload payload) {
    byte[] entity = toByteArray(SMILE_FACTORY, payload);
    URI uri = URI.create(uriStr);
    return rxHttp.post(uri, SMILE_CONTENT_TYPE, entity);
  }

  private byte[] toByteArray(JsonFactory factory, JsonPayload payload) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      JsonGenerator gen = factory.createGenerator(baos, JsonEncoding.UTF8);
      payload.toJson(gen);
      gen.close();
      baos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private void logErr(String prefix, Throwable e, int sent, int total) {
    if (LOGGER.isWarnEnabled()) {
      final Throwable cause = e.getCause() != null ? e.getCause() : e;
      String msg = String.format("%s exception %s:%s Sent %d/%d",
          prefix,
          cause.getClass().getSimpleName(), cause.getMessage(),
          sent, total);
      LOGGER.warn(msg);
      if (cause instanceof CompositeException) {
        CompositeException ce = (CompositeException) cause;
        for (Throwable t : ce.getExceptions()) {
          LOGGER.warn(" Exception {}: {}", t.getClass().getSimpleName(), t.getMessage());
        }
      }
    }
  }

  /**
   * Attempt to send all the batches totalling numMetrics in the allowed time.
   *
   * @return The total number of metrics sent.
   */
  public int sendAll(Iterable<Observable<Integer>> batches,
                     final int numMetrics, long timeoutMillis) {
    final AtomicBoolean err = new AtomicBoolean(false);
    final AtomicInteger updated = new AtomicInteger(0);
    LOGGER.debug("Got {} ms to send {} metrics", timeoutMillis, numMetrics);
    try {
      final CountDownLatch completed = new CountDownLatch(1);
      final Subscription s = Observable.mergeDelayError(Observable.from(batches))
          .timeout(timeoutMillis, TimeUnit.MILLISECONDS)
          .subscribeOn(Schedulers.immediate())
          .subscribe(updated::addAndGet, exc -> {
            logErr("onError caught", exc, updated.get(), numMetrics);
            err.set(true);
            completed.countDown();
          }, completed::countDown);
      try {
        completed.await(timeoutMillis, TimeUnit.MILLISECONDS);
      } catch (InterruptedException interrupted) {
        err.set(true);
        s.unsubscribe();
        LOGGER.warn("Timed out sending metrics. {}/{} sent", updated.get(), numMetrics);
      }
    } catch (Exception e) {
      err.set(true);
      logErr("Unexpected ", e, updated.get(), numMetrics);
    }

    if (updated.get() < numMetrics && !err.get()) {
      LOGGER.warn("No error caught, but only {}/{} sent.", updated.get(), numMetrics);
    }
    return updated.get();
  }

  /**
   * Perform an HTTP get in the allowed time.
   */
  public Response get(HttpClientRequest<ByteBuf> req, long timeout, TimeUnit timeUnit) {
    final String uri = req.getUri();
    final Response result = new Response();
    try {
      final Func1<HttpClientResponse<ByteBuf>, Observable<byte[]>> process = response -> {
        result.status = response.getStatus().code();
        result.headers = response.getHeaders();
        final Func2<ByteArrayOutputStream, ByteBuf, ByteArrayOutputStream>
            accumulator = (baos, bb) -> {
          try {
            bb.readBytes(baos, bb.readableBytes());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return baos;
        };
        return response.getContent()
            .reduce(new ByteArrayOutputStream(), accumulator)
            .map(ByteArrayOutputStream::toByteArray);
      };

      result.body = rxHttp.submit(req)
          .flatMap(process)
          .subscribeOn(Schedulers.io())
          .toBlocking()
          .toFuture()
          .get(timeout, timeUnit);
      return result;
    } catch (Exception e) {
      throw new RuntimeException("failed to get url: " + uri, e);
    }
  }
}
