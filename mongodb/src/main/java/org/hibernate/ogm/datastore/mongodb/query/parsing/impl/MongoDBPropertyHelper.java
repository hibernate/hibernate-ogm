/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.PropertyHelper;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.ComponentType;
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
		Type propertyType = getPropertyType( entityType, propertyPath );

		if ( propertyType.isEntityType() ) {
			throw new UnsupportedOperationException( "Queries on associated entities are not supported yet." );
		}

		if ( propertyType instanceof AbstractStandardBasicType ) {
			return ( (AbstractStandardBasicType<?>) propertyType ).fromString( value );
		}
		else {
			return value;
		}
	}

	private Type getPropertyType(String entityType, List<String> propertyPath) {
		OgmEntityPersister persister = getPersister( entityType );
		String propertyName = propertyPath.get( 0 );
		Type propertyType = persister.getPropertyType( propertyName );
		if ( propertyPath.size() == 1 ) {
			return propertyType;
		}
		else if ( propertyType.isComponentType() ) {
			return embeddedPropertyType( propertyPath, (ComponentType) propertyType );
		}
		throw new UnsupportedOperationException( "Queries on associated entities are not supported yet." );
	}

	private Type embeddedPropertyType(List<String> propertyPath, ComponentType propertyType) {
		Type subType = propertyType;
		for ( int i = 1; i < propertyPath.size(); i++ ) {
			ComponentType componentType = (ComponentType) subType;
			String name = propertyPath.get( i );
			int propertyIndex = componentType.getPropertyIndex( name );
			subType = componentType.getSubtypes()[propertyIndex];
			if ( subType.isAnyType() || subType.isAssociationType() || subType.isEntityType() ) {
				throw new UnsupportedOperationException( "Queries on associated entities are not supported yet." );
			}
		}
		return subType;
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

	public boolean isEmbedddedProperty(String targetTypeName, PropertyPath propertyPath) {
		List<String> namesWithoutAlias = propertyPath.getNodeNamesWithoutAlias();
		OgmEntityPersister persister = getPersister( targetTypeName );
		Type propertyType = persister.getPropertyType( namesWithoutAlias.get( 0 ) );
		return propertyType.isComponentType() && !propertyType.isAssociationType();
	}
}
