package org.hibernate.ogm.datastore.ignite.dialect.criteria.spi;

import org.hibernate.ogm.datastore.ignite.loader.criteria.impl.CriteriaCustomQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A facet for {@link GridDialect} implementations which support the execution of criteria.
 * 
 * @author Dmitriy Kozlov
 *
 */
public interface CriteriaGridDialect extends GridDialect {

	/**
	 * Returns the result of a criteria query executed on the backend.
	 *
	 * @param query the criteria to execute  
	 * @param keyMetadata metadata information common to all keys related to a given entity
	 * @return an {@link ClosableIterator} with the result of the query
	 */
	ClosableIterator<Tuple> executeCriteriaQuery(CriteriaCustomQuery query, EntityKeyMetadata keyMetadata);
	
	/**
	 * Returns the result of a criteria query with projection executed on the backend.
	 *
	 * @param query the criteria to execute  
	 * @param keyMetadata metadata information common to all keys related to a given entity
	 * @return an {@link ClosableIterator} with the result of the query
	 */
	ClosableIterator<Tuple> executeCriteriaQueryWithProjection(CriteriaCustomQuery query, EntityKeyMetadata keyMetadata);
	
}
