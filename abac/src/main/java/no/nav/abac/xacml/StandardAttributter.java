package no.nav.abac.xacml;

public final class StandardAttributter {

    /*******************************************************************************************
     * These identifiers indicate attributes of a subject.  When used, it is RECOMMENDED that they appear within an <Attributes> element of
     * the request context with a subject category (see section B.2). At most one of each of these attributes is associated with each subject.  Each attribute associated with authentication included
     * within a single <Attributes> element relates to the same authentication event.
     *******************************************************************************************/

    /**
     * This identifier indicates the name of the subject.
     */
    public static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

    /**
     *  This identifier indicates the security domain of the subject. It
     *  identifies the administrator and policy that manages the name-space in
     *  which the subject id is administered.
     */
    public static final String SUBJECT_ID_QUALIFIER = "urn:oasis:names:tc:xacml:1.0:subject:subject-id-qualifier";

    /**
     * This identifier indicates the Active Directory role of a subject
     */
    public static final String ROLE = "urn:oasis:names:tc:xacml:2.0:subject:role";
    /**
     * This identifier indicates a public key used to confirm the subject's identity.
     */
    public static final String KEY_INFO = "urn:oasis:names:tc:xacml:1.0:subject:key-info";

    /**
     * This identifier indicates the time at which the subject was authenticated.
     */
    public static final String AUTH_TIME = "urn:oasis:names:tc:xacml:1.0:subject:authentication-time";

    /**
     * This identifier indicates the method used to authenticate the subject.
     */
    public static final String AUTH_METHOD = "urn:oasis:names:tc:xacml:1.0:subject:authentication-method";

    /**
     * This identifier indicates the time at which the subject initiated the access request, according to the PEP.
     */
    public static final String REQUEST_TIME = "urn:oasis:names:tc:xacml:1.0:subject:request-time";

    /**
     *  This identifier indicates the time at which the subject's current session
     *  began, according to the PEP.
     */
    public static final String SESSION_START_TIME = "urn:oasis:names:tc:xacml:1.0:subject:session-start-time";

    /*******************************************************************************************
     * The following identifiers indicate the location where authentication
     * credentials were activated.
     *******************************************************************************************/
    /**
     *  This identifier indicates that the location is expressed as an IP address
     *  The corresponding attribute SHALL be of data-type "urn:oasis:names:tc:xacml:2.0:data-type:ipAddress ".
     */
    public static final String LOCATION_IP = "urn:oasis:names:tc:xacml:3.0:subject:authn-locality:ip-address";

    /**
     *  This identifier indicates that the location is expressed as a DNS name.
     */
    public static final String LOCATION_DNS_NAME = "urn:oasis:names:tc:xacml:3.0:subject:authn-locality:dns-name";

    /*******************************************************************************************
     * These identifiers indicate attributes of the resource.
     * When used, it is RECOMMENDED they appear within the <Attributes> element of
     * the request context with Category urn:oasis:names:tc:xacml:3.0:attribute-category:resource.
     *******************************************************************************************/

    /**
     * This attribute identifies the resource to which access is requested.
     */
    public static final String RESOURCE_ID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";


    /*******************************************************************************************
     * These identifiers indicate attributes of the action being requested.
     * When used, it is RECOMMENDED they appear within the <Attributes> element of
     * the request context with Category urn:oasis:names:tc:xacml:3.0:attribute-category:action.
     *******************************************************************************************/

    /**
     *  This attribute identifies the action for which access is requested.
     */
    public static final String ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";

    /**
     * Where the action is implicit, the value of the action-id attribute SHALL be
     */
    public static final String IMPLIED_ACTION = "urn:oasis:names:tc:xacml:1.0:action:implied-action";

    /**
     * This attribute identifies the namespace in which the action-id attribute is defined.
     */
    public static final String ACTION_NAMESPACE = "urn:oasis:names:tc:xacml:1.0:action:action-namespace";

    /*******************************************************************************************
     * These identifiers indicate attributes of the environment within which the decision request
     * is to be evaluated.  When used in the decision request, it is RECOMMENDED they appear in
     * the <Attributes> element of the request context with Category
     * urn:oasis:names:tc:xacml:3.0:attribute-category:environment.
     *******************************************************************************************/

    /**
     * This identifier indicates the current time at the context handler.
     * In practice it is the time at which the request context was created.
     * For this reason, if these identifiers appear in multiple places within a <Policy>
     * or <PolicySet>, then the same value SHALL be assigned to
     * each occurrence in the evaluation procedure, regardless of how much time
     * elapses between the processing of the occurrences.
     *
     * The corresponding attribute SHALL be of data-type "http://www.w3.org/2001/XMLSchema#time".
     */
    public static final String CURRENT_TIME = "urn:oasis:names:tc:xacml:1.0:environment:current-time";

    /**
     * The corresponding attribute SHALL be of data-type "http://www.w3.org/2001/XMLSchema#date".
     */
    public static final String CURRENT_DATE = "urn:oasis:names:tc:xacml:1.0:environment:current-date";

    /**
     * The corresponding attribute SHALL be of data-type "http://www.w3.org/2001/XMLSchema#dateTime".
     */
    public static final String CURRENT_DATETIME = "urn:oasis:names:tc:xacml:1.0:environment:current-dateTime";

}
