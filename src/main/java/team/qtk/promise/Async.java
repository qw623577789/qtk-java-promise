package team.qtk.promise;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Builder
@Getter
public class Async<T> {

    // 一定义立即执行的
    private Future<T> future;

    private Vertx vertx;

    // 只有当执行await、async、block
    @Builder.Default
    private List<Promise> deferPromises = new ArrayList<>();

    /**
     * 协程等待结果
     * 无虚拟线程上下文情况下，阻塞等待结果
     */
    @SneakyThrows
    public T await() {
        var context = Vertx.currentContext();
        if ( //非在虚拟线程上下文中，则创建虚拟线程上下文
            context == null ||
                context.threadingModel() != ThreadingModel.VIRTUAL_THREAD
        ) {
            var finalVertx = Optional.ofNullable(vertx).orElse(context == null ? Vertx.vertx() : context.owner());

            // 执行await并阻塞等待结果返回
            return Future.<T>future(promise -> ((VertxImpl) finalVertx).createVirtualThreadContext().runOnContext(v -> {
                    try {
                        if (!deferPromises.isEmpty()) deferPromises.forEach(io.vertx.core.Promise::complete);
                        promise.complete(Future.await(future));
                    } catch (Throwable error) {
                        promise.fail(new RuntimeException(error));
                    }
                })).toCompletionStage()
                .toCompletableFuture()
                .get();

        } else {
            if (!deferPromises.isEmpty()) deferPromises.forEach(io.vertx.core.Promise::complete);
            return Future.await(future);
        }
    }

    //公共
    public Async<Void> then(Consumer<T> thenFunc) {
        return Async.<Void>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        return Async.<Void>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
            .future(future.compose(lastValue -> {
                try {
                    var secondValue = thenFunc.apply(lastValue);

                    // 若有惰性resolve，则进行触发
                    if (!secondValue.getDeferPromises().isEmpty())
                        secondValue.getDeferPromises().forEach(io.vertx.core.Promise::complete);

                    return secondValue.getFuture();
                } catch (Throwable error) {
                    return Future.failedFuture(error);
                }
            }))
            .build();
    }

    public <NEW_T> Async<NEW_T> thenPromise(Supplier<Async<NEW_T>> thenFunc) {
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
            .future(future.compose(lastValue -> {
                try {
                    var secondValue = thenFunc.get();

                    // 若有惰性resolve，则进行触发
                    if (!secondValue.getDeferPromises().isEmpty())
                        secondValue.getDeferPromises().forEach(io.vertx.core.Promise::complete);

                    return secondValue.getFuture();
                } catch (Throwable error) {
                    return Future.failedFuture(error);
                }
            }))
            .build();
    }

    public <NEW_T> Async<NEW_T> thenPromise(Async<NEW_T> promise) {
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
            .future(future.compose(lastValue -> {
                try {
                    // 若有惰性resolve，则进行触发
                    if (!promise.getDeferPromises().isEmpty())
                        promise.getDeferPromises().forEach(io.vertx.core.Promise::complete);

                    return promise.getFuture();
                } catch (Throwable error) {
                    return Future.failedFuture(error);
                }
            }))
            .build();
    }

    public <NEW_T> Async<NEW_T> thenFuture(Function<T, Future<NEW_T>> thenFunc) {
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        return Async.<NEW_T>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        if (!deferPromises.isEmpty()) deferPromises.forEach(io.vertx.core.Promise::complete);
        try {
            return future.toCompletionStage().toCompletableFuture().get();
        } catch (ExecutionException error) {
            throw error.getCause();
        }
    }

    // 阻塞等待结果（设定超时）
    @SneakyThrows
    public T block(long timeout, TimeUnit unit) {
        if (!deferPromises.isEmpty()) deferPromises.forEach(io.vertx.core.Promise::complete);
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
        return Async.<T>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        return Async.<T>builder().vertx(vertx)
            .deferPromises(deferPromises)
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
        var promise = io.vertx.core.Promise.<T>promise();
        deferPromises.add(promise);
        future = promise.future();
        return this;
    }

    public void async() {
        if (!deferPromises.isEmpty()) deferPromises.forEach(io.vertx.core.Promise::complete);
    }
}