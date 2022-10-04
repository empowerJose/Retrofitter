# Retrofitter

KSP implmentation to codegen a Dagger module with Retrofit implementations of annotated interfaces.

## Example
add @Retrofitter annotation to the Retrofit interfaces
``` kotlin
@Retrofitter
interface ConfigurationInterface {

    @GET(API_PATH_LIVE_CONFIG)
    suspend fun fetchLiveConfig(): Result<LiveConfig>

    @GET(API_PATH_LIVE_CONFIG_USER)
    suspend fun fetchUserLiveConfig(): Result<UserLiveConfig>

    companion object {
        private const val API_PATH_LIVE_CONFIG = "LiveConfig"
        private const val API_PATH_LIVE_CONFIG_USER = "LiveConfig/user"
    }
}

@Retrofitter
interface CustomerDueDiligenceInterface {

    @GET(API_CUSTOMER_DUE_DILIGENCE_NEXT_QUESTION)
    suspend fun fetchNextQuestion(): Result<CustomerDueDiligenceQuestion>

    @GET(API_CUSTOMER_DUE_DILIGENCE_SUBMIT_ANSWER)
    suspend fun submitAnswer(): Result<CustomerDueDiligenceQuestion>

    companion object {
        private const val API_CUSTOMER_DUE_DILIGENCE_NEXT_QUESTION = "cdd/question/next"
        private const val API_CUSTOMER_DUE_DILIGENCE_SUBMIT_ANSWER = "cdd/question/submitanswer"
    }
}
```

The following file will be generated in `build/generated/ksp/$variant/kotlin/$package/RetrofitComponentsModule.kt`

```kotlin
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
public object RetrofitComponentsModule {
  @Provides
  @Singleton
  public fun providesConfigurationClient(retrofit: Retrofit): ConfigurationInterface =
      retrofit.create(ConfigurationInterface::class.java)

  @Provides
  @Singleton
  public fun providesCustomerDueDiligenceClient(retrofit: Retrofit): CustomerDueDiligenceInterface =
      retrofit.create(CustomerDueDiligenceInterface::class.java)
}
```

Finally, tying it all up into Dagger requires setting just `RetrofitComponentModule` as an included module the Retrofit module

```kotlin
@Module(includes = [
    RetrofitComponentsModule::class,
])
object RetrofitModule {
    @Provides
    @Singleton
    fun providesOpenRetrofitClient(
        okHttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .build()
}
```
