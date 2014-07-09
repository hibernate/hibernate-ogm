/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.ogm.OgmSessionFactory;

/**
 * Resolves {@link OgmSessionFactory} instances during JNDI look-ups as well as during de-serialization.
 *
 * @author Davide D'Alto
 */
public class OgmSessionFactoryObjectFactory implements ObjectFactory {

	@Override
	public Object getObjectInstance(Object reference, Name name, Context nameCtx, Hashtable<?, ?> environment)
			throws Exception {
		final String uuid = (String) ( (Reference) reference ).get( 0 ).getContent();
		//OgmSessionFactory does not have state so we can create a new instance each time instead of keeping a registry
		return new OgmSessionFactoryImpl( (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid ) );
	}

}
