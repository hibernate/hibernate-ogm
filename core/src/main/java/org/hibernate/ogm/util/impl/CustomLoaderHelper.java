/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.loader.impl.TupleBasedEntityLoader;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.type.Type;

/**
 * The helper contains common code from {@link org.hibernate.ogm.hibernatecore.impl.BackendCustomLoader}
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class CustomLoaderHelper {

	// At the moment we only support the case where one entity type is returned
	public static List<Object> listOfEntities(SharedSessionContractImplementor session, Type[] resultTypes, ClosableIterator<Tuple> tuples) {
		Class<?> returnedClass = resultTypes[0].getReturnedClass();
		TupleBasedEntityLoader loader = getLoader( session, returnedClass );
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( getTuplesAsList( tuples ) );
		return loader.loadEntitiesFromTuples( session, LockOptions.NONE, ogmLoadingContext );
	}

	public static List<Object> listOfEntities(SharedSessionContractImplementor session, Class<?> returnedClass, ClosableIterator<Tuple> tuples) {
		TupleBasedEntityLoader loader = getLoader( session, returnedClass );
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( getTuplesAsList( tuples ) );
		return loader.loadEntitiesFromTuples( session, LockOptions.NONE, ogmLoadingContext );
	}

	private static List<Tuple> getTuplesAsList(ClosableIterator<Tuple> tuples) {
		List<Tuple> tuplesAsList = new ArrayList<>();
		while ( tuples.hasNext() ) {
			tuplesAsList.add( tuples.next() );
		}
		return tuplesAsList;
	}

	public static TupleBasedEntityLoader getLoader(SharedSessionContractImplementor session, Class<?> entityClass) {
		OgmEntityPersister persister = (OgmEntityPersister) ( session.getFactory() ).getMetamodel().entityPersister( entityClass.getName() );
		TupleBasedEntityLoader loader = (TupleBasedEntityLoader) persister.getAppropriateLoader( LockOptions.READ, session );
		return loader;
	}
}
