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

### Exchanging TokenX on-behalf-of token

When running on NAIS and the application is configured with TokenX:
```java
TokenXOnBehalfOfTokenClient tokenClient = TokenXTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildOnBehalfOfTokenClient(); 

String accessToken = tokenClient.exchangeOnBehalfOfToken("<cluster>:<namespace>:<application>", "<access_token>"); 
```