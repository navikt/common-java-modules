package no.nav.modig.core.util;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LdapUtils {

	private static final Logger logger = LoggerFactory.getLogger(LdapUtils.class);
	
	public static String filterDNtoCNvalue(String userid) {
    	LdapName ldapname=null;    	
    	try {
    		ldapname = new LdapName(userid);
    		String cn = null;
    		for(Rdn rdn: ldapname.getRdns()){
    			if(rdn.getType().equalsIgnoreCase("CN")) {
    				cn=rdn.getValue().toString();
    				logger.debug("uid on DN form. Filtered from {} to {}", userid, cn);
    				break;
    			}    			
    		}
    		return cn;
    	} catch(InvalidNameException e) {
    		logger.debug("uid not on DN form. Skipping filter. {}", e.toString());
    		return userid;
    	}
	}
}
