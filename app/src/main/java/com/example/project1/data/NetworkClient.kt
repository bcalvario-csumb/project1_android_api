package com.example.project1.data

import okhttp3.OkHttpClient

/**
 * The app's single shared [OkHttpClient].
 *
 * OkHttp's own guidance is explicit: *"OkHttp performs best when you create a single
 * OkHttpClient instance and reuse it for all of your HTTP calls. This is because each
 * client holds its own connection pool and thread pools. Reusing connections and threads
 * reduces latency and saves memory."*
 * — https://square.github.io/okhttp/5.x/okhttp/okhttp3/-ok-http-client/
 *
 * Before this existed, `HomeViewModel`, `AdminViewModel` and `OpenPackViewModel` each
 * built their own `ProductsRepository`, and each of those constructed its own
 * `OkHttpClient()`. That meant three independent connection pools, so moving between
 * screens paid a fresh TCP + TLS handshake instead of reusing a warm connection, plus
 * three sets of idle dispatcher threads.
 *
 * `by lazy` means the client is built on first use and never rebuilt. It is thread-safe
 * by default, which matters because the three ViewModels can touch it concurrently.
 *
 * ## Timeouts
 * This deliberately keeps OkHttp's defaults (10s connect, 10s read, 10s write). That is
 * why a failing request in this app reports ~10008 ms before giving up. If you want to
 * fail faster, this object is the one place to change it:
 *
 * ```
 * OkHttpClient.Builder()
 *     .connectTimeout(5, TimeUnit.SECONDS)
 *     .readTimeout(10, TimeUnit.SECONDS)
 *     .build()
 * ```
 *
 * To add behaviour (logging, auth headers, a response cache) to a *copy* that still
 * shares this pool, use `NetworkClient.shared.newBuilder()` rather than constructing a
 * second `OkHttpClient()` — `newBuilder()` preserves the connection and thread pools.
 */
object NetworkClient {
    val shared: OkHttpClient by lazy { OkHttpClient() }
}
