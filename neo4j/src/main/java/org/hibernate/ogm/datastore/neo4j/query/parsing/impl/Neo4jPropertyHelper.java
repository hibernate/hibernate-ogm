/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.PropertyHelper;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.Type;

/**
 * Property helper dealing with Neo4j.
 *
 * @author Davide D'Alto
 */
public class Neo4jPropertyHelper implements PropertyHelper {

	private final SessionFactoryImplementor sessionFactory;
	private final EntityNamesResolver entityNames;

	public Neo4jPropertyHelper(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames) {
		this.sessionFactory = sessionFactory;
		this.entityNames = entityNames;
	}

	@Override
	public Object convertToPropertyType(String entityType, List<String> propertyPath, String value) {
		//TODO Don't invoke for params

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

	public Object convertToLiteral(String entityType, List<String> propertyPath, Object value) {
		if ( propertyPath.size() > 1 ) {
			throw new UnsupportedOperationException( "Queries on embedded/associated entities are not supported yet." );
		}
		OgmEntityPersister persister = getPersister( entityType );
		Type propertyType = persister.getPropertyType( propertyPath.get( propertyPath.size() - 1 ) );
		Object gridValue = convertToGridType( value, propertyType );
		return gridValue;
	}

	private Object convertToGridType(Object value, Type propertyType) {
		Tuple dummy = new Tuple();
		GridType gridType = typeTranslator().getType( propertyType );
		gridType.nullSafeSet( dummy, value, new String[] { "key" }, null );
		return dummy.get( "key" );
	}

	private TypeTranslator typeTranslator() {
		return sessionFactory.getServiceRegistry().getService( TypeTranslator.class );
	}

	public String getColumnName(String entityType, String propertyName) {
		return getColumnName( getPersister( entityType ), propertyName );
	}

	public String getColumnName(Class<?> entityType, String propertyName) {
		return getColumnName( (OgmEntityPersister) sessionFactory.getEntityPersister( entityType.getName() ), propertyName );
	}

	public String getColumnName(OgmEntityPersister persister, String propertyName) {
		String columnName = propertyName;
		String[] columnNames = persister.getPropertyColumnNames( columnName );
		columnName = columnNames[0];
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
