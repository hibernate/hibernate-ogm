/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.PropertyHelper;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.query.parsing.impl.ParserPropertyHelper;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Property helper dealing with MongoDB.
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 */
public class MongoDBPropertyHelper extends ParserPropertyHelper implements PropertyHelper {

	public MongoDBPropertyHelper(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames) {
		super( sessionFactory, entityNames );
	}

	public String getColumnName(Class<?> entityType, List<String> propertyName) {
		return getColumnName( (OgmEntityPersister) getSessionFactory().getEntityPersister( entityType.getName() ), propertyName );
	}

	public String getColumnName(String entityType, List<String> propertyPath) {
		return getColumnName( getPersister( entityType ), propertyPath );
	}

	public String getColumnName(OgmEntityPersister persister, List<String> propertyName) {
		String columnName = StringHelper.join( propertyName, "." );

		if ( columnName.equals( persister.getIdentifierPropertyName() ) ) {
			return MongoDBDialect.ID_FIELDNAME;
		}

		return getColumn( persister, propertyName);
	}
}
