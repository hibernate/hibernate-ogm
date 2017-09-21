/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Collections;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;

/**
 * Base class for {@link GridDialectTestHelper} that collects common implementations of the method among the dialects.
 * <p>
 * The main purpose of this class is to make it easier to update the {@link GridDialectTestHelper} implementations
 * across dialects in separate repositories.
 *
 * @author Davide D'Alto
 */
public abstract class BaseGridDialectTestHelper implements GridDialectTestHelper {

	@Override
	public void prepareDatabase(SessionFactory sessionFactory) {
	}

	@Override
	public long getNumberOfEntities(Session session) {
		return getNumberOfEntities( session.getSessionFactory() );
	}

	@Override
	public long getNumberOfAssociations(Session session) {
		return getNumberOfAssociations( session.getSessionFactory() );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public Map<String, String> getAdditionalConfigurationProperties() {
		return Collections.emptyMap();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
	}
}
