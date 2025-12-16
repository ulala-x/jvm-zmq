plugins {
    `java-library`
    application
}

description = "Sample applications demonstrating ZeroMQ Java Bindings usage"

dependencies {
    // Depend on zmq module
    implementation(project(":zmq"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(22)
}

// application 플러그인 설정 - 기본 메인 클래스
application {
    mainClass.set("io.github.ulalax.zmq.samples.PubSubSample")
}

// 모든 JavaExec 태스크에 FFM native access 권한 추가
tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// RouterToRouterSample 실행 태스크
tasks.register<JavaExec>("runRouterToRouter") {
    group = "application"
    description = "Run RouterToRouterSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.RouterToRouterSample")
}

// ReqRepSample 실행 태스크
tasks.register<JavaExec>("runReqRep") {
    group = "application"
    description = "Run ReqRepSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.ReqRepSample")
}

// PubSubSample 실행 태스크
tasks.register<JavaExec>("runPubSub") {
    group = "application"
    description = "Run PubSubSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.PubSubSample")
}

// PushPullSample 실행 태스크
tasks.register<JavaExec>("runPushPull") {
    group = "application"
    description = "Run PushPullSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.PushPullSample")
}

// PairSample 실행 태스크
tasks.register<JavaExec>("runPair") {
    group = "application"
    description = "Run PairSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.PairSample")
}

// RouterDealerSample 실행 태스크
tasks.register<JavaExec>("runRouterDealer") {
    group = "application"
    description = "Run RouterDealerSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.RouterDealerSample")
}

// ProxySample 실행 태스크
tasks.register<JavaExec>("runProxy") {
    group = "application"
    description = "Run ProxySample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.ProxySample")
}

// SteerableProxySample 실행 태스크
tasks.register<JavaExec>("runSteerableProxy") {
    group = "application"
    description = "Run SteerableProxySample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.SteerableProxySample")
}

// PollerSample 실행 태스크
tasks.register<JavaExec>("runPoller") {
    group = "application"
    description = "Run PollerSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.PollerSample")
}

// MonitorSample 실행 태스크
tasks.register<JavaExec>("runMonitor") {
    group = "application"
    description = "Run MonitorSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.MonitorSample")
}

// CurveSecuritySample 실행 태스크
tasks.register<JavaExec>("runCurveSecurity") {
    group = "application"
    description = "Run CurveSecuritySample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.CurveSecuritySample")
}

// MultipartSample 실행 태스크
tasks.register<JavaExec>("runMultipart") {
    group = "application"
    description = "Run MultipartSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.MultipartSample")
}

// RouterBenchmarkSample 실행 태스크
tasks.register<JavaExec>("runRouterBenchmark") {
    group = "application"
    description = "Run RouterBenchmarkSample"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.ulalax.zmq.samples.RouterBenchmarkSample")
}
