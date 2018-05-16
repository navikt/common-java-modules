package no.nav.modig.core.context;

import org.w3c.dom.Element;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;


public class SAMLAssertionCredential implements Destroyable {
    private boolean destroyed;
    private Element element;

    public SAMLAssertionCredential(Element element) {
        this.element = element;
    }

    public Element getElement() {
        if (destroyed) {
            throw new IllegalStateException("This credential is no longer valid");
        }
        return element;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        element = null;
        destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }
    
    @Override
    public String toString(){
    	if(destroyed){
    		return "SAMLAssertionCredential[destroyed]";
    	}
    	return "SAMLAssertionCredential[" + this.element.toString() + "]";
    }
}
