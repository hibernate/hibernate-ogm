package org.hibernate.ogm.loader;

import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.jdbc.TupleAsMapResultSet;

import java.sql.ResultSet;
import java.util.List;

/**
 * Object holding contextual information around data loading
 * and that are OGM specific. This object is used by {@link OgmLoader}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmLoadingContext {
	/**
	 * Do not edit this reference. Shared by everyone and still mutable.
	 */
	public static final OgmLoadingContext EMPTY_CONTEXT = new OgmLoadingContext();

	private TupleAsMapResultSet resultSet;

	public boolean hasResultSet() {
		return resultSet != null;
	}

	public TupleAsMapResultSet getResultSet() {
		return resultSet;
	}

	public void setTuples(List<Tuple> tuples) {
		if ( tuples == null ) {
			this.resultSet = null;
		}
		else {
			TupleAsMapResultSet tupleResultSet = new TupleAsMapResultSet();
			tupleResultSet.setTuples( tuples );
			this.resultSet = tupleResultSet;
		}
	}
}
