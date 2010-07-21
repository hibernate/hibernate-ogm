package org.hibernate.ogm.type.descriptor;

import java.util.Map;

/**
 * Contract to bind a value to the resultset
 *
 * @author Emmanuel Bernard
 */
public interface GridValueBinder<X> {
	//WrappedOptions for streams?
	void bind(Map<String,Object> resultset, X value, String[] names);
}
