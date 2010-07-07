package org.hibernate.ogm.type.descriptor;

import java.util.Map;

/**
 * Extract value from the result set
 *
 * @author Emmanuel Bernard
 */
public interface GridValueExtractor<X> {
	//WrappedOptions for streams?
	X extract(Map<String, Object> resultset, String name);
}
