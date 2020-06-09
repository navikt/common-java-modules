package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.*;

public class MockXacmlRequest {

    static final String OIDC_TOKEN_BODY = "abc";
    static final String SAML_TOKEN = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48c2FtbDI6QXNzZXJ0aW9uIHhtbG5zOnNhbWwyPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIiBJRD0iU0FNTC03YTI5MDY1Yi02MTgzLTQ5MzMtODIyMS02NGRiMDI3ODMzMTMiIElzc3VlSW5zdGFudD0iMjAxNy0wNy0wNFQxNjoxMjoyNloiIFZlcnNpb249IjIuMCI+PHNhbWwyOklzc3Vlcj5JUzAyPC9zYW1sMjpJc3N1ZXI+PFNpZ25hdHVyZSB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+DQo8U2lnbmVkSW5mbz4NCiAgPENhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz4NCiAgPFNpZ25hdHVyZU1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNyc2Etc2hhMSIvPg0KICA8UmVmZXJlbmNlIFVSST0iI1NBTUwtN2EyOTA2NWItNjE4My00OTMzLTgyMjEtNjRkYjAyNzgzMzEzIj4NCiAgICA8VHJhbnNmb3Jtcz4NCiAgICAgIDxUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIvPg0KICAgICAgPFRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPg0KICAgIDwvVHJhbnNmb3Jtcz4NCiAgICA8RGlnZXN0TWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3NoYTEiLz4NCiAgICA8RGlnZXN0VmFsdWU+SmtsMVZXZHdhZERyZGZldktURU5hbndvei8wPTwvRGlnZXN0VmFsdWU+DQogIDwvUmVmZXJlbmNlPg0KPC9TaWduZWRJbmZvPg0KICAgIDxTaWduYXR1cmVWYWx1ZT5YOEw2N3QrY05SSHZINm43cFpSc3RKSWZ5UjYybWJSL01LOUZ1L3BKOWJtQ1ZLRk1DVVFQbDRRdGR4TVJRekx4c1VyWndVV0h4Z204TW5zNVpuWk9PMVBVajJndk9oRXhjbElGQ0dUS052a1JyYjZtR09uTVM0K0VQQ3N2SWRiWmRjZTYvZkpSYjRsMmVuZEk4cVhTY1dmQUtLc0lwYU1ndEI5Y2IyTklOcWRiMktrYnpUbUMvK2JQTkVDbHBZS1Mxa0NZZVlSYWFmN1hqYzhsV2p5QUhDY25JdXF2QlZmZEh6c1ArMU54RzB5MDdjTUNGemV5cVU1NXExMERaZ21vOW1EOG5saUV1azRNYjRsT3Y1MEdFWURTRVErT1BLSEF5OXN4YTR3bUNQaFk0N1FXSU5NWnBnRTllZG93QnZ2YXZlWE5mQ0syTzVVRzdhYmdIYytaeEE9PTwvU2lnbmF0dXJlVmFsdWU+PEtleUluZm8+PFg1MDlEYXRhPjxYNTA5Q2VydGlmaWNhdGU+TUlJR2t6Q0NCWHVnQXdJQkFnSVRHZ0FBSUd3SGtGc1FCK25LSmdBQ0FBQWdiREFOQmdrcWhraUc5dzBCQVFzRkFEQk5NUlV3RXdZS0NaSW1pWlB5TEdRQkdSWUZiRzlqWVd3eEZEQVNCZ29Ka2lhSmsvSXNaQUVaRmdSMFpYTjBNUjR3SEFZRFZRUURFeFZFTWpZZ1NYTnpkV2x1WnlCRFFTQkpiblJsY200d0hoY05NVGN3TXpFek1EZ3dOekE1V2hjTk1Ua3dNekV6TURnd056QTVXakI0TVFzd0NRWURWUVFHRXdKT1R6RU5NQXNHQTFVRUNCTUVUMU5NVHpFTk1Bc0dBMVVFQnhNRVQxTk1UekVqTUNFR0ExVUVDaE1hUVZKQ1JVbEVVeTBnVDBjZ1ZrVk1Sa1ZTUkZORlZFRlVSVTR4RHpBTkJnTlZCQXNUQms1QlZpQkpWREVWTUJNR0ExVUVBd3dNS2k1MFpYTjBMbXh2WTJGc01JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBeStsWmhsVktySXNvMVZTYVpGaDR2eHlzZGcxZnNpMk82eU0weUxnaVJDZWhHTENmSFJYNnBxUzF3dkFSS2dhYm5rM3RPOFMyc20wVlp5MnFIK3Q1OEZKdUtwTXk1WUtsTzJNSDZ2YTMxNGZYV21VZ1dEdU80YnJSem56d1dQWEd3ajBUYURrUEp6VlhUTEN2cWg5eXBaYlUyb2UxZVc0OXJ3TzZEblRUeTY2cjhQU094U0FKS05wdElLdnBiMzE4OURUNDhVRmhiNTRDRDdPSlBrQk9LclUxRHBOYkdyL3p6LzVjUHpkdCt5TndNV0paQUE2aFhQZjlTYXpmWjYvT2syWTVLV2hQdHB2TU1zUGx3Ti8yeDh2RjR2Q00rdmsxN29ZNHRib3BWMjhQWUs4ZlJ5LzNlYmpKUGtldDlZaDR5b0hhdWZKOG1PNDdhcWtOTnhOdlRRSURBUUFCbzRJRFB6Q0NBenN3SFFZRFZSME9CQllFRkJLS3NUYys1cDJhQyszTE9KbnRZdjNGWVV1dE1COEdBMVVkSXdRWU1CYUFGUDJUelFPQ0pZZkRIYStEOURuTlZFZ0dQTHExTUlJQkd3WURWUjBmQklJQkVqQ0NBUTR3Z2dFS29JSUJCcUNDQVFLR2djUnNaR0Z3T2k4dkwyTnVQVVF5TmlVeU1FbHpjM1ZwYm1jbE1qQkRRU1V5TUVsdWRHVnliaXhEVGoxRU1qWkVVbFpYTURVeExFTk9QVU5FVUN4RFRqMVFkV0pzYVdNbE1qQnJaWGtsTWpCVFpYSjJhV05sY3l4RFRqMVRaWEoyYVdObGN5eERUajFEYjI1bWFXZDFjbUYwYVc5dUxFUkRQWFJsYzNRc1JFTTliRzlqWVd3L1kyVnlkR2xtYVdOaGRHVlNaWFp2WTJGMGFXOXVUR2x6ZEQ5aVlYTmxQMjlpYW1WamRFTnNZWE56UFdOU1RFUnBjM1J5YVdKMWRHbHZibEJ2YVc1MGhqbG9kSFJ3T2k4dlkzSnNMblJsYzNRdWJHOWpZV3d2WTNKc0wwUXlOaVV5TUVsemMzVnBibWNsTWpCRFFTVXlNRWx1ZEdWeWJpNWpjbXd3Z2dGWEJnZ3JCZ0VGQlFjQkFRU0NBVWt3Z2dGRk1JRzVCZ2dyQmdFRkJRY3dBb2FCckd4a1lYQTZMeTh2WTI0OVJESTJKVEl3U1hOemRXbHVaeVV5TUVOQkpUSXdTVzUwWlhKdUxFTk9QVUZKUVN4RFRqMVFkV0pzYVdNbE1qQnJaWGtsTWpCVFpYSjJhV05sY3l4RFRqMVRaWEoyYVdObGN5eERUajFEYjI1bWFXZDFjbUYwYVc5dUxFUkRQWFJsYzNRc1JFTTliRzlqWVd3L1kwRkRaWEowYVdacFkyRjBaVDlpWVhObFAyOWlhbVZqZEVOc1lYTnpQV05sY25ScFptbGpZWFJwYjI1QmRYUm9iM0pwZEhrd0p3WUlLd1lCQlFVSE1BR0dHMmgwZEhBNkx5OXZZM053TG5SbGMzUXViRzlqWVd3dmIyTnpjREJlQmdnckJnRUZCUWN3QW9aU2FIUjBjRG92TDJOeWJDNTBaWE4wTG14dlkyRnNMMk55YkM5RU1qWkVVbFpYTURVeExuUmxjM1F1Ykc5allXeGZSREkySlRJd1NYTnpkV2x1WnlVeU1FTkJKVEl3U1c1MFpYSnVLRElwTG1OeWREQU9CZ05WSFE4QkFmOEVCQU1DQmFBd1BRWUpLd1lCQkFHQ054VUhCREF3TGdZbUt3WUJCQUdDTnhVSWd2ejZVb2JTMmtpRDJaVThoUHFVY29LZDFsSVNnOHV6WUlPc25FUUNBV1FDQVFnd0V3WURWUjBsQkF3d0NnWUlLd1lCQlFVSEF3RXdHd1lKS3dZQkJBR0NOeFVLQkE0d0REQUtCZ2dyQmdFRkJRY0RBVEFOQmdrcWhraUc5dzBCQVFzRkFBT0NBUUVBNXJNN0RCRitBL3RrT09wWG9POFlKbnJ2VmdseHgrb2U1dEZ2YzJkVU81M21HL0JZb1RLbG1ZZGdGai8xK1BuWWdodmdwTGdCOENwY3RpcEJQWGZ0VDlQak1qVS93cFdLMS84WG83eVdVSkJhbVRMYmVpK1E4a0dJUmEzY213eS9UZG0wR0xrdkJOa01QdFM5bml2bnZ2b2hvTlg2bk9DSWhhTGEwWkJpV1FHVW15R3JhcU9lZHJDS25Fc0Z1dGViaXZHR3hOVGtvS1JhdWhVbHBvVHpuQmJDb3IxVmliUXpJYWduc1JmUDl3RVZEZVFPVXkvUDNPa3RTU21yQ1M3a0RGcUY2aUlvLzRwVlR6cUtvMnpOWFFZUXp1M1pTcmVPaXhWYWNNS2RpRU5JeXlWZ3o4aE0yaWcrbkhWdmNXeFBpMFJGOXNtblc4V0RuRGozRmlrQ1J3PT08L1g1MDlDZXJ0aWZpY2F0ZT48WDUwOUlzc3VlclNlcmlhbD48WDUwOUlzc3Vlck5hbWU+Q049RDI2IElzc3VpbmcgQ0EgSW50ZXJuLCBEQz10ZXN0LCBEQz1sb2NhbDwvWDUwOUlzc3Vlck5hbWU+PFg1MDlTZXJpYWxOdW1iZXI+NTc5ODE5NDE4MjU4MDEzNTQxNDI0MTQxNjgzNzI4ODcyNjY4NzI4OTI2MzE2PC9YNTA5U2VyaWFsTnVtYmVyPjwvWDUwOUlzc3VlclNlcmlhbD48L1g1MDlEYXRhPjwvS2V5SW5mbz48L1NpZ25hdHVyZT48c2FtbDI6U3ViamVjdD48c2FtbDI6TmFtZUlEIEZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6dW5zcGVjaWZpZWQiPjA2MDQ3MjIwMjAxPC9zYW1sMjpOYW1lSUQ+PHNhbWwyOlN1YmplY3RDb25maXJtYXRpb24gTWV0aG9kPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6Y206YmVhcmVyIj48c2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbkRhdGEgTm90QmVmb3JlPSIyMDE3LTA3LTA0VDE2OjEyOjIzWiIgTm90T25PckFmdGVyPSIyMDE3LTA3LTA0VDE3OjEyOjI5WiIvPjwvc2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbj48L3NhbWwyOlN1YmplY3Q+PHNhbWwyOkNvbmRpdGlvbnMgTm90QmVmb3JlPSIyMDE3LTA3LTA0VDE2OjEyOjIzWiIgTm90T25PckFmdGVyPSIyMDE3LTA3LTA0VDE3OjEyOjI5WiIvPjxzYW1sMjpBdHRyaWJ1dGVTdGF0ZW1lbnQ+PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJpZGVudFR5cGUiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6dXJpIj48c2FtbDI6QXR0cmlidXRlVmFsdWU+RWtzdGVybkJydWtlcjwvc2FtbDI6QXR0cmlidXRlVmFsdWU+PC9zYW1sMjpBdHRyaWJ1dGU+PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJhdXRoZW50aWNhdGlvbkxldmVsIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OnVyaSI+PHNhbWwyOkF0dHJpYnV0ZVZhbHVlPjQ8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjxzYW1sMjpBdHRyaWJ1dGUgTmFtZT0iY29uc3VtZXJJZCIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDp1cmkiPjxzYW1sMjpBdHRyaWJ1dGVWYWx1ZT5zcnZ2ZWlsYXJiYWt0aXZpdGU8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjwvc2FtbDI6QXR0cmlidXRlU3RhdGVtZW50Pjwvc2FtbDI6QXNzZXJ0aW9uPg==";
    static final String SUBJECT_ID = "A111111";
    static final String DOMAIN = "Foreldrepenger";
    static final AbacPersonId FNR = AbacPersonId.fnr("01010122222");
    static final String CREDENTIAL_RESOURCE = "srvEksempelPep";

    public static XacmlRequest getXacmlRequest() {
        return new XacmlRequest().withRequest(getRequest());
    }

    public static XacmlRequest getXacmlRequestWithSubjectAttributes() {
        return new XacmlRequest().withRequest(getRequestWithSubjectAttributes());
    }

    public static XacmlRequest getXacmlRequestWithSubjAttrWithoutEnvironment() {
        return new XacmlRequest().withRequest(getRequestWithSubjAttrWithoutEnvironment());
    }

    public static XacmlRequest getXacmlRequestForSubjectWithKode7Resource() {
        return new XacmlRequest().withRequest(getRequestWithoutResource().withResource(XacmlRequestBuilder.lagKode7Resource("veilarb")));
    }

    public static XacmlRequest getXacmlRequestForSubjectWithKode6Resource() {
        return new XacmlRequest().withRequest(getRequestWithoutResource().withResource(XacmlRequestBuilder.lagKode6Resource("veilarb")));
    }

    public static XacmlRequest getXacmlRequestForSubjectWithEgenAnsattResource() {
        return new XacmlRequest().withRequest(getRequestWithoutResource().withResource(XacmlRequestBuilder.lagEgenAnsattResource("veilarb")));
    }

    public static XacmlRequest getXacmlRequestForSubjectWithVeilArbResource() {
        return new XacmlRequest().withRequest(getRequestWithoutResource().withResource(XacmlRequestBuilder.lagVeilArbResource("veilarb")));
    }

    private static Request getRequestWithActionAndResource() {
        final Action action = new Action();
        action.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:action:action-id", "read"));

        final Resource resource = new Resource();
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.resource_type", "no.nav.abac.attributter.resource.felles.person"));
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.domene", DOMAIN));
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.person.fnr", FNR.getId()));

        return new Request()
                .withAction(action)
                .withResource(resource);
    }

    static Request getRequest() {
        final Environment environment = new Environment();
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.oidc_token_body", OIDC_TOKEN_BODY));
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.pep_id", CREDENTIAL_RESOURCE));

        return getRequestWithActionAndResource().withEnvironment(environment);
    }

    static Request getSAMLRequest() {
        final Environment environment = new Environment();
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.saml_token", SAML_TOKEN));
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.pep_id", CREDENTIAL_RESOURCE));

        return getRequestWithActionAndResource().withEnvironment(environment);
    }


    static Request getRequestWithSubjectAttributes() {
        final Environment environment = new Environment();
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.pep_id", CREDENTIAL_RESOURCE));

        final AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:subject:subject-id", SUBJECT_ID));
        accessSubject.getAttribute().add(new Attribute("no.nav.abac.attributter.subject.felles.subjectType", "InternBruker"));

        return getRequestWithActionAndResource()
                .withAccessSubject(accessSubject)
                .withEnvironment(environment);
    }

    private static Request getRequestWithSubjAttrWithoutEnvironment() {
        final AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:subject:subject-id", SUBJECT_ID));
        accessSubject.getAttribute().add(new Attribute("no.nav.abac.attributter.subject.felles.subjectType", "InternBruker"));

        return getRequestWithActionAndResource().withAccessSubject(accessSubject);
    }

    private static Request getRequestWithoutResource() {
        final Environment environment = new Environment();
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.pep_id", CREDENTIAL_RESOURCE));

        final Action action = new Action();
        action.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:action:action-id", "read"));

        final AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:subject:subject-id", SUBJECT_ID));
        accessSubject.getAttribute().add(new Attribute("no.nav.abac.attributter.subject.felles.subjectType", "InternBruker"));

        return new Request().withAccessSubject(accessSubject).withEnvironment(environment).withAction(action);
    }
}
