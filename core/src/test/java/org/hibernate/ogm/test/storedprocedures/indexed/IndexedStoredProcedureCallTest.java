/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures.indexed;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.ogm.backendtck.storedprocedures.indexed.IndexedStoredProcDialect;
import org.hibernate.ogm.backendtck.storedprocedures.indexed.IndexedStoredProcProvider;
import org.hibernate.ogm.backendtck.storedprocedures.indexed.IndexedStoredProcedure;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class IndexedStoredProcedureCallTest extends org.hibernate.ogm.backendtck.storedprocedures.indexed.IndexedStoredProcedureCallTest {
	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		Properties properties = info.getProperties();
		properties.setProperty( OgmProperties.DATASTORE_PROVIDER, IndexedStoredProcProvider.class.getName() );
		// function with one parameter and result as list of entities
		IndexedStoredProcDialect.FUNCTIONS.put( TEST_RESULT_SET_STORED_PROC, new IndexedStoredProcedure() {

			@Override
			public ClosableIterator<Tuple> execute(Object[] params) {
				List<Tuple> result = new ArrayList<>( 1 );
				Tuple resultTuple = new Tuple();
				resultTuple.put( "id", params[0] );
				resultTuple.put( "title", params[1] );
				result.add( resultTuple );
				return CollectionHelper.newClosableIterator( result );
			}
		} );
		// function with one parameter and returned simple value
		IndexedStoredProcDialect.FUNCTIONS.put( TEST_SIMPLE_VALUE_STORED_PROC, new IndexedStoredProcedure() {

			@Override
			public ClosableIterator<Tuple> execute(Object[] params) {
				List<Tuple> result = new ArrayList<>( 1 );
				Tuple resultTuple = new Tuple();
				resultTuple.put( "result", params[0] );
				result.add( resultTuple );
				return CollectionHelper.newClosableIterator( result );
			}
		} );
	}
}
