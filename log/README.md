Log
---
The log module contains various components to standardise some log
elements across NAV.

Log statement correlation
---------------
When requests hit several services, it is important to be 
able to track the requests. A correlation id should be either retrieved
from incoming requests, or generated if absent. The correlation id should
then be added to all log-statements to enable you or your consumers to track 
requests in your app.

However, some services may be hit several times by upstream
services. This may result in several requests with the same correlation
id. To be able to identify unique requests in a single service,
all requests should also log a unique request id.

These ids need to be put on ThreadLocal (MDC) when a request hits the 
server, and removed when the request is finished. ```LogFilter``` 
provides this functionality.

The correlation id needs to be passed on to downstream services. This
currently implemented in the [common-java-modules/rest](../rest), 
```no.nav.common.rest.RestUtils```.

Log configuration inspection
----------------------------
```LoginfoServlet``` lists loggers with levels that differs from the root 
logger. Don't expose this in production, as it leaks implementation details

GDPR compliance
------------
```MaskedLoggingEvent``` and ```MaskingAppender``` replaces potential
"fødselsnummere" with asterisks, "*"

Constants
---------
The log filters, both the servlet filter (```LogFilter```) and the 
jersey filter (in ```no.nav.common.rest.RestUtils```) uses

Utils
-----
Various logging utils provided through LogUtils

Default logback configuration
-----------------------------
2 default log configurations are provided

* ```logback-default.xml``` - includes abac logging configuration, and 
requires the [common-java-modules/abac](../abac) to be present on your classpath
* ```logback-default-without-abac.xml``` - 
 
These configurations can be used 
by adding 

```xml
<include resource="logback-default-without-abac.xml"/>
```
to your logback xml