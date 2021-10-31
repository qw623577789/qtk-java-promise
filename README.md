YTK-Java-Promise
================
基于[jasync-multiny](https://github.com/qw623577789/jasync-mutiny)封装的类似ES的Promise异步对象,支持async-await模式，实现**异步执行async模式、同步阻塞返回block模式**，关键能**让开发者以传统顺序的方式编写异步代码！！！！**

## Installation

```groovy
//gradle
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.qw623577789:promise:v1.0.0'
    annotationProcessor 'io.github.vipcxj:jasync-core:0.1.3'
    testAnnotationProcessor 'io.github.vipcxj:jasync-core:0.1.3'
}
```

## Features
- 类似ES的Promise.resolve/reject/all/allSettled/race/any/then...catch...finally...功能
- 支持惰性resolve模式，即只有真正等到触发``async()、block()、awiat()``时，才触发Promise.resolve
- 支持[smallrye-mutiny](https://smallrye.io/smallrye-mutiny/index.html)的Event-Driven Reactive库的``Uni``、[Vertx](https://vertx.io/)的``Future/Consumer<Handler<T>>/CompositeFuture``转为Promise对象，满足在异步数据处理场景的大部分需求
- 支持类似ES6的 **Promise.resolve().then().then()** 链式写法与ES7的 **Promise.resolve().await()**

## Function

- **JPromise\<Void> resolve()** 包装null值的Promise
- **JPromise\<T> resolve(T value)** 包装对象成Promise
- **JPromise\<T> resolve(Future\<T> future)** 将[Vertx.Future](https://vertx.io/docs/vertx-core/java/#_future_results)异步对象包装成Promise,并可等待获取结果值
- **JPromise<List\<T>> resolve(CompositeFuture future)** 将[Vertx.CompositeFuture](https://vertx.io/docs/vertx-core/java/#_future_coordination)【vertx下的promise.all/race/any/join功能的实现】异步对象包装成Promise,并可依次返回结果
- **JPromise\<T> resolve(Consumer<Handler\<T>> consumer)** 包装一个lamda函数，当传入的参数(是一个方法)被调用时，返回结果值
- **JPromise\<T> resolve(Uni\<T> value)** 包装[smallrye-mutiny.Uni](https://smallrye.io/smallrye-mutiny/getting-started/creating-unis)异步对象成Promise,并可等待获取结果值
- **JPromise\<?> defer(Supplier<T> deferFunc)** 包装lamda表达式成Promise对象，当触发``async()、block()、await()``时，才执行lamda方法触发Promise.resolve
- **JPromise\<List\<Object>> all(JPromise<?>... promises)** 并发执行多个Promise，并将结果依次返回。**若其中一个Promise抛错，则将终止等待所有Promise结果并立即抛出错误**
- **JPromise\<List\<Object>> allSettled(JPromise<?>... promises)** 并发执行多个Promise，并将结果依次返回。**将等待所有Promise结果返回(无论是正常返回还是抛错)，返回列表里每个item为正常数据或者error**
- **JPromise\<Object> race(JPromise<?>... promises)** 并发执行多个Promise，**当其中某个Promise最先出结果时(正常返回或者抛错)，立即返回该结果**。
- **JPromise\<Object> any(JPromise<?>... promises)** 并发执行多个Promise，当其中某个Promise出**正常结果时(抛错则跳过，继续等待)，立即返回该结果**。
- **<T> JPromise<T> reject()** 异步抛出``RuntimeException``错误
- **<T> JPromise<T> reject(String errorMessage)** 异步抛出``RuntimeException(errorMessage)``错误
- **<T> JPromise<T> reject(Throwable t)** 异步抛出自定义错误



## Usage

- then方法必须返回Promise对象
- 使用``await()``方法的函数必须加``@Async()``注解，并且返回Promise对象

#### Promise链式异步非阻塞写法

```java
void test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    Promise
        .resolve(sleep3s.future())
        .then(sleep3sTimerId -> Promise.resolve(sleep5s.future()))
        .then(sleep5sTimerId -> {
            System.out.println("sleep5sTimerId is:" + sleep5sTimerId);
            return Promise.resolve();
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
        .then(sleep3sTimerId -> Promise.resolve(sleep5s.future()))
        .block();

    System.out.println("sleep5sTimerId is:" + sleep5sTimerId);
}
```

#### Promise链式then...catch...finally写法
```java
void test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    Promise
        .resolve(sleep3s.future())
        .then(() -> {
            return Promise.reject("在这里抛了个错。。。");
        })
        .then(sleep3sTimerId -> Promise.resolve(sleep5s.future()))
        .then(sleep5sTimerId -> {
            System.out.println("sleep5sTimerId is:" + sleep5sTimerId);
            return Promise.resolve();
        })
        .doCatch(Exception.class, error -> {
            System.out.println("抓到了一个错误:" + error.getMessage());
        })
        .doFinally(() -> {
            System.out.println("finally");
            return Promise.resolve();
        })
        .async();
}
```

#### await异步非阻塞写法

```java
@Async
private JPromise<Void> test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    long sleep3sTimerId = Promise.resolve(sleep3s.future()).await();

    System.out.println("sleep3sTimerId is:" + sleep3sTimerId);

    long sleep5sTimerId = Promise.resolve(sleep5s.future()).await();

    System.out.println("sleep5sTimerId is:" + sleep5sTimerId);

    // @Async注解的函数必须返回Promise
    return Promise.resolve(); 
}

// 调用
test().async();
```

#### await异步阻塞写法

```java
@Async
private JPromise<Long> test() {
    io.vertx.core.Promise<Long> sleep3s = io.vertx.core.Promise.promise();
    io.vertx.core.Promise<Long> sleep5s = io.vertx.core.Promise.promise();
    vertx.setTimer(3000, timerId -> sleep3s.complete(timerId)); //睡眠3s后返回timerId
    vertx.setTimer(5000, timerId -> sleep5s.complete(timerId)); //睡眠5s后返回timerId

    long sleep3sTimerId = Promise.resolve(sleep3s.future()).await();

    System.out.println("sleep3sTimerId is:" + sleep3sTimerId);

    long sleep5sTimerId = Promise.resolve(sleep5s.future()).await();

    System.out.println("sleep5sTimerId is:" + sleep5sTimerId);

    return Promise.resolve(sleep5sTimerId)
}

// 调用
System.out.println(test().block()); //输出sleep5sTimerId值
```

#### await try...catch...写法

```java
@Async
private JPromise<Void> test() {
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
    }
    catch(Exception error) {
        System.out.println("抓到了一个错误:" + error.getMessage());
    }

    // @Async注解的函数必须返回Promise
    return Promise.resolve(); 
}

// 调用
test().async();
```

#### 终止执行异步方法

```java
@Async
private JPromise<Void> test() {
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
    }
    catch(Exception error) {
        System.out.println("抓到了一个错误:" + error.getMessage());
    }

    // @Async注解的函数必须返回Promise
    return Promise.resolve(); 
}

Handle task = test().async();

System.out.println("isCanceled:" + task.isCanceled);

// 4秒后终止异步任务
vertx.setTimer(4000, timerId -> task.cancel());

```

## Notice
- 本组件仅支持javac(Sun JDK)编译器，不支持ecj(elicpise for Java)编译器
- *(此条idea用户可以忽略,vscode用户必看)* 虽然vscode-java组件**运行、调试、跑测试用例**时用的是ecj编译器，但项目做了专门适配，需要安装改造后的[Test Runner for Java
](https://github.com/qw623577789/vscode-java-test)插件以支持在vscode里运行或者调试, 配置详情见``.vscode``文件夹里的配置与``build.gradle``里的