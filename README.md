QTK-Java-Promise
================

2.X版本基于 ``Vertx.Future-JDK虚拟线程``封装的类似ES的Promise异步对象,支持block-async-await模式，实现**异步执行async模式、同步阻塞返回block模式**，关键能**让开发者以传统顺序的方式编写异步代码！！！！**

## 安装

```groovy
//gradle
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.qw623577789:qtk-java-promise:2.0.0'
}
```

## 特性

- 类似ES的Promise.resolve/reject/all/allSettled/race/any/then...catch...finally...功能
- 支持惰性resolve模式，即只有真正等到触发 ``async()、block()、awiat()``时，才触发Promise.resolve
- 支持[Vertx](https://vertx.io/)的 ``Future/Consumer<Handler<T>>/CompositeFuture``转为Promise对象，满足在异步数据处理场景的大部分需求
- 支持类似ES6的 **Promise.resolve().then().then()...catch...finally...** 链式写法与ES7的 **Promise.resolve().await()**
- 相对于1.X版本，对代码无入侵，无额外注解，原生支持jdk新语法

## 方法

- **Async\<Void> resolve()** 包装null值的Promise
- **Async\<T> resolve(T value)** 包装对象成Promise
- **Async\<T> resolve(Supplier\<T> supplier)** 包装异步函数,作用等同于 ``Promise.resolve().then(supplier)``
- **Async\<T> resolve(Future\<T> future)** 将[Vertx.Future](https://vertx.io/docs/vertx-core/java/#_future_results)异步对象包装成Promise,并可等待获取结果值
- **Async<List\<T>> resolve(CompositeFuture future)** 将[Vertx.CompositeFuture](https://vertx.io/docs/vertx-core/java/#_future_coordination)【vertx下的promise.all/race/any/join功能的实现】异步对象包装成Promise,并可依次返回结果
- **Async\<T> resolve(Consumer<Handler\<T>> consumer)** 包装一个lambda函数，当传入的参数(是一个方法)被调用时，返回结果值
- **Async\<T> deferResolve()** 开启惰性resolve模式，与 ``Promise.resolve()``相比，即只有真正等到触发 ``async()、block()、awiat()``时，才触发执行链。
- **Async\<List\<Object>> all(Async<?>... promises)** 并发执行多个Promise，并将结果依次返回。 **若其中一个Promise抛错，则将终止等待所有Promise结果并立即抛出错误**
- **Async\<List\<Object>> allSettled(Async<?>... promises)** 并发执行多个Promise，并将结果依次返回。**将等待所有Promise结果返回(无论是正常返回还是抛错)，返回列表里每个item为正常数据或者error**
- **Async\<Object> race(Async<?>... promises)** 并发执行多个Promise，**当其中某个Promise最先出结果时(正常返回或者抛错)，立即返回该结果**。
- **Async\<Object> any(Async<?>... promises)** 并发执行多个Promise，当其中某个Promise出**正常结果时(抛错则跳过，继续等待)，立即返回该结果**。
- **Async\<List\<T>> allSameType(Async<T>... promises)** 并发执行多个**同类型Promise**，并将结果依次返回。**若其中一个Promise抛错，则将终止等待所有Promise结果并立即抛出错误**
- **Async\<List\<T>> allSettledSameType(Async<T>... promises)** 并发执行多个**同类型Promise**，并将结果依次返回。**将等待所有Promise结果返回(无论是正常返回还是抛错)，返回列表里每个item为正常数据或者error**
- **Async\<T> raceSameType(Async<T>... promises)** 并发执行多个**同类型Promise**，**当其中某个Promise最先出结果时(正常返回或者抛错)，立即返回该结果**。
- **Async\<T> anySameType(Async<T>... promises)** 并发执行多个**同类型Promise**，当其中某个Promise出*正常结果时(抛错则跳过，继续等待)，立即返回该结果*
- **<T> Async<T> reject()** 异步抛出 ``RuntimeException``错误
- **<T> Async<T> reject(String errorMessage)** 异步抛出 ``RuntimeException(errorMessage)``错误
- **<T> Async<T> reject(Throwable t)** 异步抛出自定义错误
- Promise.resolve().then(xx).thenFuture(xx).thenPromise(xxx).doCatch(xxx).finally(xxx)
- ``block()/block(long timeout, TimeUnit unit)``同步阻塞模式、``async()``异步非阻塞模式、``await()``异步阻塞模式

## 用法

更多例子可以看测试用例

- then方法返回值必须为同步对象(大部分对象都是这个)
- thenPromise方法返回值必须为Async对象
- thenFuture方法返回值必须为Vertx.Future对象

**await依赖了Vertx的 ``VirtualThreadContext``上下文，在运行环境没有Vertx实例、或者不在 ``VirtualThreadContext``上下文情况下，会自动创建 ``VirtualThreadContext``上下文。 但是每次await自动创建 ``VirtualThreadContext``上下文并不是一个好的选择，这会导致 ``ThreadLocal.get/set``、``Vertx.currentContext().get/put``在返回类型为Async的函数里没法正常工作(可以理解为已经切换到其他线程)**，且实际是阻塞当前线程模式运行

正确使用应如下：

```jshelllanguage
ThreadLocal threadLocal = new ThreadLocal();

// 包在虚拟线程上下文内执行所有的await，保证所有await都在同一个虚拟线程内运行
((VertxImpl) vertx).createVirtualThreadContext().runOnContext(v -> { 
     threadLocal.set("使用threadLocal存储值");
     Vertx.currentContext().put("key", "使用threadLocal存储值");
     xxxx1.await(); 
     xxxx2.await();
     method1.await();
     method2();
     System.out.println(threadLocal.get()); //输出"使用threadLocal存储值"
     System.out.println(Vertx.currentContext().get("key")); //输出"使用Vertx.currentContext存储值"
 });
 
Async<String> method1() {
    //输出"使用threadLocal存储值"，没上述套在runOnContext里的话，method1会在新的上下文执行，这里将取不到值
    System.out.println(threadLocal.get());

    //输出"使用Vertx.currentContext存储值"，没上述套在runOnContext里的话，method1会在新的上下文执行，这里将取不到值
    System.out.println(Vertx.currentContext().get("key")); 
    return Promise.resolve("method1");
}

String method2() {
    System.out.println(threadLocal.get()); //输出"使用threadLocal存储值"
    System.out.println(Vertx.currentContext().get("key")); //输出"使用Vertx.currentContext存储值"
    xxxx3.await();
    return "method1";
  } 
```

项目中使用，假设web框架可以这样封装使用

```java
package team.qtk.promise;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;

import java.util.function.Function;

public class T {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(request -> {

            // 开启一个虚拟线程处理当前请求
            ((VertxImpl) vertx).createVirtualThreadContext().runOnContext(v -> {
                Promise.resolve(request.body())
                    .then(body -> { return handler(body.toJsonObject());})
                    .then(responseJson -> {
                        HttpServerResponse response = request.response();
                        response.putHeader("content-type", "text/plain");
                        response.setStatusCode(200).end(responseJson.toString());
                    })
                    .doCatch(error -> {
                        HttpServerResponse response = request.response();
                        response.putHeader("content-type", "text/plain");
                        response.setStatusCode(500).end(error.getMessage());
                    })
                    .async();
            });
        });

        server.listen(8080);
    }

    static JsonObject handler(JsonObject body) {
        System.out.println(body.toString());
        //xxxx.await(); 此时已经在虚拟线程上下文中，不会再创建新的上下文
        return body;
    }
}
```

#### Promise链式异步非阻塞写法

```java
void test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    Promise
        .resolve(sleep3s.future())
        .thenFuture(sleep3sTimerId -> sleep5s.future())
        .then(sleep5sTimerId -> {
            System.out.println("sleep5sTimerId is:" + sleep5sTimerId);
            return sleep5sTimerId;
        })
        .async();
}
```

#### Promise链式异步阻塞写法

```java
void test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    long sleep5sTimerId = Promise
        .resolve(sleep3s.future())
        .thenFuture(sleep3sTimerId -> sleep5s.future())
        .block();

    System.out.println("sleep5sTimerId is:" + sleep5sTimerId);
}
```

#### Promise链式then...catch...finally写法

void test() {
io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

```
Promise
    .resolve(sleep3s.future())
    .then(() -> {
        throw new RuntimeException("在这里抛了个错。。。");
    })
    .thenFuture(sleep3sTimerId -> sleep5s.future())
    .then(sleep5sTimerId -> {
        System.out.println("sleep5sTimerId is:" + sleep5sTimerId);
        return sleep5sTimerId;
    })
    .doCatch(error -> {
        System.out.println("抓到了一个错误:" + error.getMessage());
    })
    .doFinally(() -> {
        System.out.println("finally");
    })
    .async();
```

}

#### await异步非阻塞写法

```java

private Async<Void> test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    long sleep3sTimerId = Promise.resolve(sleep3s.future()).await();

    System.out.println("sleep3sTimerId is:" + sleep3sTimerId);

    long sleep5sTimerId = Promise.resolve(sleep5s.future()).await();

    System.out.println("sleep5sTimerId is:" + sleep5sTimerId);
}

// 最好在一个``VirtualThreadContext``调用
test().async();
```

#### await异步阻塞写法

```java

private Async<Long> test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    long sleep3sTimerId = Promise.resolve(sleep3s.future()).await();

    System.out.println("sleep3sTimerId is:" + sleep3sTimerId);

    long sleep5sTimerId = Promise.resolve(sleep5s.future()).await();

    System.out.println("sleep5sTimerId is:" + sleep5sTimerId);

    return Promise.resolve(sleep5sTimerId);
}

// 最好在一个``VirtualThreadContext``调用
//输出sleep5sTimerId值，block为阻塞当前线程模式获取结果，test方法里的await是非阻塞模式运行
System.out.println(test().block()); 
```

#### await try...catch...写法

```java

private test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    long sleep3sTimerId = Promise.resolve(sleep3s.future()).await();

    System.out.println("sleep3sTimerId is:" + sleep3sTimerId);

    long sleep5sTimerId = Promise.resolve(sleep5s.future()).await();

    System.out.println("sleep5sTimerId is:" + sleep5sTimerId);

    try {
        Promise.reject("在这里抛了个错。。。").await();
    } catch (Exception error) {
        System.out.println("抓到了一个错误:" + error.getMessage());
    }
}

// 在一个``VirtualThreadContext``调用
test();
```

## Notice

- 本组件仅支持JDK21以上的版本
- 与1.x版本相比，不写 ``async()、block()、awiat()``也会自动异步执行！！！，其实就是跟nodejs的async函数一样了
