/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.datastore.impl.SchemaDefinerInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Central definition of the standard set of initiators defined by OGM for the {@link SessionFactoryServiceRegistry}.
 *
 * @see OgmSessionFactoryServiceRegistryImpl
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OgmSessionFactoryServiceInitiators {

	public static List<SessionFactoryServiceInitiator<?>> LIST = Collections.unmodifiableList( Arrays.<SessionFactoryServiceInitiator<?>>asList(
			QueryParserServicesInitiator.INSTANCE,
			SchemaDefinerInitiator.INSTANCE,
			NativeNoSqlQueryInterpreterInitiator.INSTANCE
	) );
}
