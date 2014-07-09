/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;

/**
 * Resolves entity names into Class references using the metadata
 * from the Hibernate SessionFactory.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class SessionFactoryEntityNamesResolver implements EntityNamesResolver {

	private final SessionFactoryImplementor sessionFactory;
	private final ClassLoaderService classLoaderService;

	public SessionFactoryEntityNamesResolver(SessionFactory sessionFactory) {
		this.sessionFactory = (SessionFactoryImplementor) sessionFactory;
		this.classLoaderService = this.sessionFactory.getServiceRegistry().getService( ClassLoaderService.class );
	}

	@Override
	public Class<?> getClassFromName(String entityName) {
		return classLoaderService.classForName( sessionFactory.getImportedClassName( entityName ) );
	}
}
