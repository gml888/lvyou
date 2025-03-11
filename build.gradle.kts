import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.gradle.internal.classpath.Instrumented.systemProperty

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.moying"
version = "1.0.0"

repositories {
  mavenCentral()
}

val vertxVersion = "4.5.13"
val junitJupiterVersion = "5.9.1"

val mainVerticleName = "com.moying.lvyou.Main"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClass.set(launcherClassName)
}
sourceSets {
  main {
    resources {
      setSrcDirs(listOf("src/main/resources"))
    }
  }
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-mysql-client")
  implementation("io.vertx:vertx-web-client:4.5.13")
  implementation("io.vertx:vertx-auth-jwt:4.5.13")

  implementation("io.smallrye.reactive:smallrye-mutiny-vertx-core:3.17.1")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1") // 请根据需要选择合适的版本
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")


  //用于生成二维码
  implementation("com.google.zxing:core:3.5.1")
  implementation("com.google.zxing:javase:3.5.1")

 // implementation("io.vertx:vertx-config") // 核心配置模块
  implementation("io.vertx:vertx-config-yaml") // YAML配置模块
  // Log4j2 核心组件
  //implementation("org.apache.logging.log4j:log4j-api:2.20.0")
  //implementation("org.apache.logging.log4j:log4j-core:2.20.0")
  // SLF4J 到 Log4j2 的桥接（关键）
  //implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
  implementation("org.eclipse.microprofile.openapi:microprofile-openapi-api:2.0") // 用于实体定义
  implementation("javax.persistence:javax.persistence-api:2.2") // 用于实体定义
  implementation("com.google.code.gson:gson:2.12.1") // 用于数据处理
  implementation("io.netty:netty-resolver-dns-native-macos:+:osx-aarch_64") //mac m1 arm64 使用
  compileOnly("org.projectlombok:lombok:1.18.30") // 用于实体定义
  annotationProcessor("org.projectlombok:lombok:1.18.30") // 用于实体定义
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf(
    "run",
    mainVerticleName,
    "--redeploy=$watchForChange",
    "--launcher-class=$launcherClassName",
    "--on-redeploy=$doOnChange"
  )
}
// 定义不同的运行任务
tasks.register<JavaExec>("runDev") {
  group = "Application"
  description = "Run the application in development mode"
  mainClass.set(launcherClassName)
  classpath = sourceSets.main.get().runtimeClasspath
  args = listOf(
    "run",
    mainVerticleName,
    "--redeploy=$watchForChange",
    "--launcher-class=$launcherClassName",
    "--on-redeploy=$doOnChange",
    "-Denv=dev"
  )
}

tasks.register<JavaExec>("runTest") {
  group = "Application"
  description = "Run the application in test mode"
  mainClass.set(launcherClassName)
  classpath = sourceSets.main.get().runtimeClasspath
  args = listOf(
    "run",
    mainVerticleName,
    "--redeploy=$watchForChange",
    "--launcher-class=$launcherClassName",
    "--on-redeploy=$doOnChange",
    "-Denv=test"
  )
}

tasks.register<JavaExec>("runProd") {
  group = "Application"
  description = "Run the application in production mode"
  mainClass.set(launcherClassName)
  classpath = sourceSets.main.get().runtimeClasspath
  args = listOf(
    "run",
    mainVerticleName,
    "--redeploy=$watchForChange",
    "--launcher-class=$launcherClassName",
    "--on-redeploy=$doOnChange",
    "-Denv=prod"
  )
}
// 定义打包任务
tasks.register<ShadowJar>("shadowJarProd") {
  group = "Build"
  description = "Create a fat JAR for production"
  archiveClassifier.set("prod-fat")
  manifest {
    attributes(
      mapOf(
        "Main-Class" to launcherClassName, // 指定启动类
        "Main-Verticle" to mainVerticleName, // 指定主Verticle
      )
    )
  }
  from(sourceSets.main.get().output) // 确保包含编译后的代码和资源文件
  configurations = listOf(project.configurations.runtimeClasspath.get()) // 包含运行时依赖
  mergeServiceFiles()
}
