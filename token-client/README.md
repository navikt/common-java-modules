# Token Client

Provides clients for creating machine-to-machine and exchanging on-behalf-of tokens.

## How to use

Add the following dependency:
```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>token-client</artifactId>
    <version>NEWEST_VERSION</version>
</dependency>
```

### Creating Azure AD machine-to-machine token

When running on NAIS and the application is configured with Azure AD:
```java
AzureAdMachineToMachineTokenClient tokenClient = AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient();
    
String accessToken = tokenClient.createMachineToMachineToken("api://<cluster>.<namespace>.<application>/.default");
```

### Exchanging Azure AD on-behalf-of token

When running on NAIS and the application is configured with Azure AD:
```java
AzureAdOnBehalfOfTokenClient tokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildOnBehalfOfTokenClient();

String accessToken = tokenClient.exchangeOnBehalfOfToken("api://<cluster>.<namespace>.<application>/.default", "<access_token>");
```

#### Azure AD OBO cache key strategy

The default cache key remains scope plus subject, and Caffeine remains the default cache.
To isolate cached tokens by incoming assertion, client ID, token endpoint and scope, opt in:

```java
AzureAdOnBehalfOfTokenClient tokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .withOnBehalfOfCacheKeyStrategy(AzureAdOnBehalfOfCacheKeyStrategy.ASSERTION_HASH)
        .buildOnBehalfOfTokenClient();
```

The assertion-based key contains only a versioned prefix and a SHA-256 hash.
Different incoming tokens for the same user get different keys, including after token renewal.
Applications must validate incoming tokens before calling the token client.
This strategy does not provide shared storage; a shared `TokenCache` selected through `withCache(...)`
must additionally isolate applications and environments with a namespace.
This option does not change machine-to-machine or TokenX clients.

For a shared Valkey implementation, see
[`token-client-cache-valkey`](../token-client-cache-valkey/README.md).

### Exchanging TokenX on-behalf-of token

When running on NAIS and the application is configured with TokenX:
```java
TokenXOnBehalfOfTokenClient tokenClient = TokenXTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildOnBehalfOfTokenClient(); 

String accessToken = tokenClient.exchangeOnBehalfOfToken("<cluster>:<namespace>:<application>", "<access_token>"); 
```