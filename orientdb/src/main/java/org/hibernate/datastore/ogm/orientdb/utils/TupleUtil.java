/*
* Hibernate OGM, Domain model persistence for NoSQL datastores
* 
* License: GNU Lesser General Public License (LGPL), version 2.1 or later
* See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
*/

package org.hibernate.datastore.ogm.orientdb.utils;

import com.orientechnologies.orient.core.id.ORecordId;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.hibernate.datastore.ogm.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.dialect.spi.TupleContext;

public class TupleUtil {

	public static List<String> loadClassPropertyNames(Connection connection, ORecordId rid) throws SQLException {
		List<String> classPropertyNames = new LinkedList<>();

		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery( "select from " + rid );
		if ( rs.next() ) {
			ResultSetMetaData metadata = rs.getMetaData();
			for ( int i = 0; i < metadata.getColumnCount(); i++ ) {
				int fn = i + 1;
				classPropertyNames.add( metadata.getColumnName( fn ) );
			}
		}

		return classPropertyNames;
	}
@Deprecated
	public static Set<String> getFieldsForJoin(TupleContext tupleContext, Collection<String> classPropertyNames) {
		Set<String> systemAndClassProperties = new HashSet<>( 20 );
		systemAndClassProperties.addAll( classPropertyNames );
		systemAndClassProperties.addAll( OrientDBConstant.SYSTEM_FIELDS );
		List<String> list = new LinkedList<>( tupleContext.getSelectableColumns() );
		list.removeAll( systemAndClassProperties );
		return new HashSet<String>( list );
	}

}
