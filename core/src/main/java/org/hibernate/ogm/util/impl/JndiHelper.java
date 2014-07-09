/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.naming.Context;

import org.hibernate.cfg.Environment;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JndiHelper {
	public static Properties extractJndiProperties(Map configurationValues) {
		final Properties jndiProperties = new Properties();

		for ( Map.Entry entry : (Set<Map.Entry>) configurationValues.entrySet() ) {
			if ( !String.class.isInstance( entry.getKey() ) ) {
				continue;
			}
			final String propertyName = (String) entry.getKey();
			final Object propertyValue = entry.getValue();
			if ( propertyName.startsWith( Environment.JNDI_PREFIX ) ) {
				// write the IntialContextFactory class and provider url to the result only if they are
				// non-null; this allows the environmental defaults (if any) to remain in effect
				if ( Environment.JNDI_CLASS.equals( propertyName ) ) {
					if ( propertyValue != null ) {
						jndiProperties.put( Context.INITIAL_CONTEXT_FACTORY, propertyValue );
					}
				}
				else if ( Environment.JNDI_URL.equals( propertyName ) ) {
					if ( propertyValue != null ) {
						jndiProperties.put( Context.PROVIDER_URL, propertyValue );
					}
				}
				else {
					final String passThruPropertyname = propertyName.substring( Environment.JNDI_PREFIX.length() + 1 );
					jndiProperties.put( passThruPropertyname, propertyValue );
				}
			}
		}

		return jndiProperties;
	}
}
