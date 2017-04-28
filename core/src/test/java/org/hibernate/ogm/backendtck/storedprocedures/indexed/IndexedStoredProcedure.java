package org.hibernate.ogm.backendtck.storedprocedures.indexed;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public interface IndexedStoredProcedure {
	Object execute(Object[] params);
}
