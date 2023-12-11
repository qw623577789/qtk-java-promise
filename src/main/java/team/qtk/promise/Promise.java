/*
 * This Java source file was generated by the Gradle 'init' task.
 * Promise只有在() -> { return Promise(); } 才能切换执行线程
 */
package team.qtk.promise;

import io.vertx.core.*;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.impl.VertxImpl;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

//
@Builder
public class Promise {

    @Builder
    @Getter
    public static class Async<T> {

        // 一定义立即执行的
        private Future<T> future;

        // 只有当执行await、async、block
        private io.vertx.core.Promise deferPromise;

        /**
         * 协程等待结果
         * 无虚拟线程上下文情况下，阻塞等待结果
         */
        @SneakyThrows
        public T await() {

            if ( //非在虚拟线程上下文中，则创建虚拟线程上下文
                Vertx.currentContext() == null ||
                    Vertx.currentContext().threadingModel() != ThreadingModel.VIRTUAL_THREAD
            ) {
                // 获取当前上下文
                var context = Vertx.currentContext();

                // 若上下文都获取不到，则创建Vertx实例并创建上下文
                if (context == null) context = Vertx.vertx().getOrCreateContext();

                //  获取vertx实例
                VertxImpl vertx = (VertxImpl) context.owner();

                // 执行await并阻塞等待结果返回
                return Future.<T>future(promise -> vertx.createVirtualThreadContext().runOnContext(v -> {
                        try {
                            if (deferPromise != null) deferPromise.complete();
                            promise.complete(Future.await(future));
                        } catch (Throwable error) {
                            promise.fail(new RuntimeException(error));
                        }
                    })).toCompletionStage()
                    .toCompletableFuture()
                    .get();

            } else {
                if (deferPromise != null) deferPromise.complete();
                return Future.await(future);
            }
        }

        //公共
        public Async<Void> then(Consumer<T> thenFunc) {
            return Async.<Void>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        thenFunc.accept(lastValue);
                        return Future.succeededFuture();

                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public Async<Void> then(Runnable thenFunc) {
            return Async.<Void>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        thenFunc.run();
                        return Future.succeededFuture();

                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }
        //

        public <NEW_T> Async<NEW_T> then(Function<@NonNull T, NEW_T> thenFunc) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        var secondValue = thenFunc.apply(lastValue);
                        if (secondValue instanceof Future<?>) {
                            throw new RuntimeException("then里返回Future的话,请用thenFuture包装下");
                        } else if (secondValue instanceof Async<?>) {
                            throw new RuntimeException("then里返回Promise的话,请用thenPromise包装下");
                        }
                        return Future.succeededFuture(secondValue);
                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public <NEW_T> Async<NEW_T> then(Supplier<NEW_T> thenFunc) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        var secondValue = thenFunc.get();
                        if (secondValue instanceof Future<?>) {
                            throw new RuntimeException("then里返回Future的话,请用thenFuture包装下");
                        } else if (secondValue instanceof Async<?>) {
                            throw new RuntimeException("then里返回Promise的话,请用thenPromise包装下");
                        }
                        return Future.succeededFuture(secondValue);

                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public <NEW_T> Async<NEW_T> thenPromise(Function<T, Async<NEW_T>> thenFunc) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        var secondValue = thenFunc.apply(lastValue);
                        return secondValue.getFuture();
                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public <NEW_T> Async<NEW_T> thenPromise(Supplier<Async<NEW_T>> thenFunc) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        var secondValue = thenFunc.get();
                        return secondValue.getFuture();
                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public <NEW_T> Async<NEW_T> thenPromise(Async<NEW_T> thenFunc) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        return thenFunc.getFuture();
                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public <NEW_T> Async<NEW_T> thenFuture(Function<T, Future<NEW_T>> thenFunc) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        return thenFunc.apply(lastValue);
                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public <NEW_T> Async<NEW_T> thenFuture(Supplier<Future<NEW_T>> thenFunc) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        return thenFunc.get();
                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        public <NEW_T> Async<NEW_T> thenFuture(Future<NEW_T> thenFuture) {
            return Async.<NEW_T>builder()
                .deferPromise(deferPromise)
                .future(future.compose(lastValue -> {
                    try {
                        return thenFuture;
                    } catch (Throwable error) {
                        return Future.failedFuture(error);
                    }
                }))
                .build();
        }

        // 阻塞等待结果（无限制等待）
        @SneakyThrows
        public T block() {
            if (deferPromise != null) deferPromise.complete();
            try {
                return future.toCompletionStage().toCompletableFuture().get();
            } catch (ExecutionException error) {
                throw error.getCause();
            }
        }

        // 阻塞等待结果（设定超时）
        @SneakyThrows
        public T block(long timeout, TimeUnit unit) {
            if (deferPromise != null) deferPromise.complete();
            try {
                return future.toCompletionStage().toCompletableFuture().get(timeout, unit);
            } catch (ExecutionException error) {
                throw error.getCause();
            }
        }

        /**
         * 捕获错误返回null
         *
         * @param errorFunc
         * @return
         */
        public Async<T> doCatch(Consumer<Throwable> errorFunc) {
            return Async.<T>builder()
                .future(future.otherwise(throwable -> { //用otherwise不用recover
                    try {
                        errorFunc.accept(throwable);
                        return null;
                    } catch (Throwable error) {
                        throw error;
                    }
                }))
                .build();
        }

        /**
         * 捕获错误并返回新值
         * 值类型必须等于一个Promise的返回值类型
         *
         * @param errorFunc
         * @return
         */
        public Async<T> doCatch(Function<Throwable, T> errorFunc) {
            return Async.<T>builder()
                .future(future.otherwise(throwable -> { //用otherwise不用recover原因是，recover报错的话也会回调otherwise
                    try {
                        return errorFunc.apply(throwable);
                    } catch (Throwable error) {
                        throw error;
                    }
                }))
                .build();
        }

        public Async<T> doFinally(Runnable finallyFunc) {
            future.eventually(() -> {
                finallyFunc.run();
                return Future.succeededFuture();
            });
            return this;
        }

        /**
         * 惰性执行promise
         * 只有await、block、async调用时才会触发
         *
         * @return
         */
        public Async<T> deferResolve() {
            // 创建一个promise，后续可以使用complete()触发
            deferPromise = io.vertx.core.Promise.promise();
            future = deferPromise.future();
            return this;
        }

        public void async() {
            if (deferPromise != null) deferPromise.complete();
        }
    }

    public static Async<Void> resolve() {
        return resolve((Void) null);
    }

    public static <T> Async<T> resolve(T value) {
        return Async.<T>builder().future(Future.succeededFuture(value)).build();
    }

    /**
     * 执行异步函数
     * 等同于Promise.resolve().then(supplier)
     */
    public static <T> Async<T> resolve(Supplier<T> supplier) {
        return resolve().then(supplier);
    }

    /**
     * 【当前线程】异步等待Vertx.Future返回
     * Vertx.Future在是一旦定义就立即触发
     */
    public static <T> Async<T> resolve(Future<T> future) {
        return Async.<T>builder().future(future).build();
    }

    /**
     * 【当前线程】异步等待Vertx.CompositeFuture返回，并将每个Vertx.Future结果按顺序放入List
     * 若想指定Vertx.CompositeFuture执行线程请使Promise.resolve(RunOn.xxx, () -> Promise.resolve(Vertx.CompositeFuture))
     */

    public static <Object> Async<List<Object>> resolve(CompositeFuture future) {
        var newFuture = Future.<List<Object>>future(promise -> future.onComplete(
            h -> {
                if (h.succeeded()) {
                    promise.complete(h.result().list());
                } else {
                    promise.fail(
                        h.cause() instanceof NoStackTraceThrowable ?
                            new RuntimeException(h.cause().getMessage()) :
                            h.cause()
                    );
                }
            }
        ));
        return resolve(newFuture);
    }

    /**
     * 【当前线程】包装一个lambda函数，当传入的参数(一个方法(Vertx.Handler))被调用时，返回结果值
     * 例如:Promise.<Long>resolve(doneCallback ->  Vertx.vertx().setTimer(3000, doneCallback))
     */
    public static <T> Async<T> resolve(Consumer<Handler<T>> consumer) {
        var newFuture = Future.<T>future(promise -> {
            try {
                consumer.accept(promise::complete);
            } catch (Throwable error) {
                promise.fail(error);
            }
        });

        return resolve(newFuture);
    }

    /**
     * 并发执行多个【同类型Promise】，并将结果依次返回。
     * 若其中一个Promise抛错，则将终止等待所有Promise结果并立即抛出错误
     */
    public static <T> Async<List<T>> allSameType(List<Async<T>> promises) {
        var futures = promises.stream().map(Async::getFuture).toList();

        var newFuture = Future.<List<T>>future(promise -> Future.all(futures).onComplete(
            h -> {
                if (h.succeeded()) {
                    promise.complete(h.result().list());
                } else {
                    promise.fail(
                        h.cause() instanceof NoStackTraceThrowable ?
                            new RuntimeException(h.cause().getMessage()) :
                            h.cause()
                    );
                }
            }
        ));
        return resolve(newFuture);
    }

    /**
     * 并发执行多个【同类型Promise】，并将结果依次返回。
     * 若其中一个Promise抛错，则将终止等待所有Promise结果并立即抛出错误
     */
    @SafeVarargs
    public static <T> Async<List<T>> allSameType(Async<T>... promises) {
        return allSameType(Arrays.asList(promises));
    }

    /**
     * 并发执行多个【同类型Promise】，并将结果依次返回。
     * 将等待所有Promise结果返回(无论是正常返回还是抛错)，返回列表里每个item为正常数据或者error
     */
    public static <T> Async<List<T>> allSettledSameType(List<Async<T>> promises) {
        var futures = promises.stream()
            .map(Async::getFuture)
            .map(f -> f.recover(throwable -> throwable instanceof NoStackTraceThrowable ?
                Future.succeededFuture((T) new RuntimeException(throwable.getMessage())) :
                Future.succeededFuture((T) throwable))) //出错的future转为正确的
            .toList();

        var newFuture = Future.<List<T>>future(promise -> Future.join(futures).onComplete(
            h -> {
                if (h.succeeded()) {
                    promise.complete(h.result().list());
                } else {
                    promise.fail(
                        h.cause() instanceof NoStackTraceThrowable ?
                            new RuntimeException(h.cause().getMessage()) :
                            h.cause()
                    );
                }
            }
        ));
        return resolve(newFuture);
    }

    /**
     * 并发执行多个【同类型Promise】，并将结果依次返回。
     * 将等待所有Promise结果返回(无论是正常返回还是抛错)，返回列表里每个item为正常数据或者error
     */
    @SafeVarargs
    public static <T> Async<List<T>> allSettledSameType(Async<T>... promises) {
        return allSettledSameType(Arrays.asList(promises));
    }

    /**
     * 并发执行多个【同类型Promise】，当其中某个Promise最先出结果时(正常返回或者抛错)，立即返回该结果。
     */
    public static <T> Async<T> raceSameType(List<Async<T>> promises) {
        var futures = promises.stream()
            .map(Async::getFuture)
            .map(f -> f.recover(throwable -> throwable instanceof NoStackTraceThrowable ?
                Future.succeededFuture((T) new RuntimeException(throwable.getMessage())) :
                Future.succeededFuture((T) throwable))) //出错的future转为正确的
            .toList();

        var newFuture = Future.<T>future(promise -> Future.any(futures).onComplete(
            h -> {
                if (h.succeeded()) {
                    var nowResult = h.result();
                    Object first = null;

                    // 寻找第一个完成的元素
                    for (var i = 0; i < futures.size(); i++) {
                        if (nowResult.isComplete(i)) {
                            first = nowResult.resultAt(i);
                            break;
                        }
                    }

                    if (first instanceof Throwable firstError) {
                        promise.fail(
                            firstError instanceof NoStackTraceThrowable ?
                                new RuntimeException(firstError.getMessage()) :
                                firstError
                        );
                    } else {
                        promise.complete((T) first);
                    }
                } else {
                    promise.fail(
                        h.cause() instanceof NoStackTraceThrowable ?
                            new RuntimeException(h.cause().getMessage()) :
                            h.cause()
                    );
                }
            }
        ));
        return resolve(newFuture);
    }

    /**
     * 并发执行多个【同类型Promise】，当其中某个Promise最先出结果时(正常返回或者抛错)，立即返回该结果。
     */
    @SafeVarargs
    public static <T> Async<T> raceSameType(Async<T>... promises) {
        return raceSameType(Arrays.asList(promises));
    }

    /**
     * 并发执行多个【同类型Promise】，当其中某个Promise出正常结果时(抛错则跳过，继续等待)，立即返回该结果
     */
    public static <T> Async<T> anySameType(List<Async<T>> promises) {
        var futures = promises.stream().map(Async::getFuture).toList();

        var newFuture = Future.<T>future(promise -> Future.any(futures).onComplete(
            h -> {
                if (h.succeeded()) {
                    var nowResult = h.result();
                    Object first = null;

                    // 寻找第一个完成的元素
                    for (var i = 0; i < futures.size(); i++) {
                        if (nowResult.isComplete(i)) {
                            first = nowResult.resultAt(i);
                            break;
                        }
                    }

                    promise.complete((T) first);
                } else {
                    promise.fail(
                        h.cause() instanceof NoStackTraceThrowable ?
                            new RuntimeException(h.cause().getMessage()) :
                            h.cause()
                    );
                }
            }
        ));
        return resolve(newFuture);
    }

    /**
     * 并发执行多个【同类型Promise】，当其中某个Promise出正常结果时(抛错则跳过，继续等待)，立即返回该结果
     */
    @SafeVarargs
    public static <T> Async<T> anySameType(Async<T>... promises) {
        return anySameType(Arrays.asList(promises));
    }

    /**
     * 并发执行多个Promise，并将结果依次返回。
     * 若其中一个Promise抛错，则将终止等待所有Promise结果并立即抛出错误
     */
    public static Async<List<Object>> all(List<Async<?>> promises) {
        return allSameType((List) promises);
    }

    /**
     * 并发执行多个Promise，并将结果依次返回。
     * 若其中一个Promise抛错，则将终止等待所有Promise结果并立即抛出错误
     */
    public static Async<List<Object>> all(Async<?>... promises) {
        return all(Arrays.asList(promises));
    }

    /**
     * 并发执行多个Promise，并将结果依次返回。
     * 将等待所有Promise结果返回(无论是正常返回还是抛错)，返回列表里每个item为正常数据或者error
     */
    public static Async<List<Object>> allSettled(List<Async<?>> promises) {
        return allSettledSameType((List) promises);
    }

    /**
     * 并发执行多个Promise，并将结果依次返回。
     * 将等待所有Promise结果返回(无论是正常返回还是抛错)，返回列表里每个item为正常数据或者error
     */
    public static Async<List<Object>> allSettled(Async<?>... promises) {
        return allSettled(Arrays.asList(promises));
    }

    /**
     * 并发执行多个Promise，当其中某个Promise最先出结果时(正常返回或者抛错)，立即返回该结果。
     */
    public static Async<Object> race(Async<?>... promises) {
        return race(Arrays.asList(promises));
    }

    /**
     * 并发执行多个Promise，当其中某个Promise最先出结果时(正常返回或者抛错)，立即返回该结果。
     */
    public static Async<Object> race(List<Async<?>> promises) {
        return raceSameType((List) promises);
    }

    /**
     * 并发执行多个Promise，当其中某个Promise出正常结果时(抛错则跳过，继续等待)，立即返回该结果
     */
    public static Async<Object> any(List<Async<?>> promises) {
        return anySameType((List) promises);
    }

    /**
     * 并发执行多个Promise，当其中某个Promise出正常结果时(抛错则跳过，继续等待)，立即返回该结果
     */
    public static Async<Object> any(Async<?>... promises) {
        return any(Arrays.asList(promises));
    }

    /**
     * 抛出RuntimeException("reject")错误
     */
    public static Async<RuntimeException> reject() {
        return reject("reject");
    }

    /**
     * 抛出RuntimeException(errorMessage)错误
     */
    public static Async<RuntimeException> reject(String errorMessage) {
        return Async.<RuntimeException>builder()
            .future(Future.failedFuture(new RuntimeException(errorMessage)))
            .build();
    }

    /**
     * 抛出自定义错误
     */
    public static <T extends Throwable> Async<T> reject(T t) {
        return Async.<T>builder()
            .future(Future.failedFuture(t))
            .build();
    }

    public static Async<Void> deferResolve() {
        return Async.<Void>builder().build().deferResolve();
    }

}
