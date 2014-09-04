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
import org.hibernate.ogm.persister.spi.OgmEntityPersister;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.Type;

/**
 * Property helper dealing with MongoDB.
 *
 * @author Gunnar Morling
 */
public class MongoDBPropertyHelper implements PropertyHelper {

	private final SessionFactoryImplementor sessionFactory;
	private final EntityNamesResolver entityNames;

	public MongoDBPropertyHelper(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames) {
		this.sessionFactory = sessionFactory;
		this.entityNames = entityNames;
	}

	@Override
	public Object convertToPropertyType(String entityType, List<String> propertyPath, String value) {
		if ( propertyPath.size() > 1 ) {
			throw new UnsupportedOperationException( "Queries on embedded/associated entities are not supported yet." );
		}

		OgmEntityPersister persister = getPersister( entityType );

		Type propertyType = persister.getPropertyType( propertyPath.get( propertyPath.size() - 1 ) );

		if ( propertyType instanceof AbstractStandardBasicType ) {
			return ( (AbstractStandardBasicType<?>) propertyType ).fromString( value );
		}
		else {
			return value;
		}
	}

	public String getColumnName(String entityType, String propertyName) {
		return getColumnName( getPersister( entityType ), propertyName );
	}

	public String getColumnName(Class<?> entityType, String propertyName) {
		return getColumnName(
				(OgmEntityPersister) sessionFactory.getEntityPersister( entityType.getName() ),
				propertyName
		);
	}

	public String getColumnName(OgmEntityPersister persister, String propertyName) {
		String columnName = propertyName;

		if ( columnName.equals( persister.getIdentifierPropertyName() ) ) {
			columnName = MongoDBDialect.ID_FIELDNAME;
		}
		else {
			String[] columnNames = persister.getPropertyColumnNames( columnName );
			columnName = columnNames[0];
		}

		return columnName;
	}

	private OgmEntityPersister getPersister(String entityType) {
		Class<?> targetedType = entityNames.getClassFromName( entityType );
		if ( targetedType == null ) {
			throw new IllegalStateException( "Unknown entity name " + entityType );
		}

		return (OgmEntityPersister) sessionFactory.getEntityPersister( targetedType.getName() );
	}

}
