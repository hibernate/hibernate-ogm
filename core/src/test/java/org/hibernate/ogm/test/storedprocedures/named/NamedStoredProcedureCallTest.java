/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures.named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.ogm.backendtck.storedprocedures.named.NamedStoredProcDialect;
import org.hibernate.ogm.backendtck.storedprocedures.named.NamedStoredProcProvider;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NamedStoredProcedureCallTest extends org.hibernate.ogm.backendtck.storedprocedures.named.NamedStoredProcedureCallTest {

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		Properties properties = info.getProperties();
		properties.setProperty( OgmProperties.DATASTORE_PROVIDER, NamedStoredProcProvider.class.getName() );
		// function with one parameter and result as list of entities
		NamedStoredProcDialect.FUNCTIONS.put( TEST_RESULT_SET_STORED_PROC, ( Map<String, Object> params ) -> {
			List<Tuple> result = new ArrayList<>( 1 );
			Tuple resultTuple = new Tuple();
			resultTuple.put( "id", params.get( TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME ) );
			resultTuple.put( "title", params.get( TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME ) );
			result.add( resultTuple );
			return CollectionHelper.newClosableIterator( result );
		} );
		// function with one parameter and returned simple value
		NamedStoredProcDialect.FUNCTIONS.put( TEST_SIMPLE_VALUE_STORED_PROC, ( Map<String, Object> params ) -> {
			List<Tuple> result = new ArrayList<>( 1 );
			Tuple resultTuple = new Tuple();
			resultTuple.put( "result", params.get( TEST_SIMPLE_VALUE_STORED_PROC_PARAM_NAME ) );
			result.add( resultTuple );
			return CollectionHelper.newClosableIterator( result );
		} );
	}
}
