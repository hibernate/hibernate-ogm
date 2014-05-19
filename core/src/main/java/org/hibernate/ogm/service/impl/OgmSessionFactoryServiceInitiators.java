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

import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.dialect.impl.GridDialectInitiator;
import org.hibernate.ogm.options.navigation.impl.OptionsServiceInitiator;
import org.hibernate.ogm.type.impl.TypeTranslatorInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Central definition of the standard set of initiators defined by OGM for the
 * {@link org.hibernate.service.spi.SessionFactoryServiceRegistry}
 *
 * @see OgmSessionFactoryServiceRegistryImpl
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OgmSessionFactoryServiceInitiators {

	public static List<SessionFactoryServiceInitiator<?>> LIST = Collections.unmodifiableList( Arrays.<SessionFactoryServiceInitiator<?>>asList(
			TypeTranslatorInitiator.INSTANCE,
			OptionsServiceInitiator.INSTANCE ,
			DatastoreProviderInitiator.INSTANCE,
			GridDialectInitiator.INSTANCE,
			QueryParserServicesInitiator.INSTANCE
	) );

}
