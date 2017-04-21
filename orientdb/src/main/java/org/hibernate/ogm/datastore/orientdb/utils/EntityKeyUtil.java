/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.orientdb.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.mapping.Column;
import org.hibernate.ogm.model.key.spi.EntityKey;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class EntityKeyUtil {

	private static final Log log = LoggerFactory.getLogger();

	public static boolean isEmbeddedColumn(Column column) {
		return isEmbeddedColumn( column.getName() );
	}

	public static boolean isEmbeddedColumn(String column) {
		return column.contains( "." );
	}

	public static void setFieldValue(StringBuilder queryBuffer, Object dbKeyValue) {
		if ( dbKeyValue instanceof String || dbKeyValue instanceof UUID || dbKeyValue instanceof Character ) {
			queryBuffer.append( "'" ).append( dbKeyValue ).append( "'" );
		}
		else if ( dbKeyValue instanceof Date || dbKeyValue instanceof Calendar ) {
			Calendar calendar = null;
			if ( dbKeyValue instanceof Date ) {
				calendar = Calendar.getInstance();
				calendar.setTime( (Date) dbKeyValue );
			}
			else if ( dbKeyValue instanceof Calendar ) {
				calendar = (Calendar) dbKeyValue;
			}
			String formattedStr = ( FormatterUtil.getDateTimeFormater().get() ).format( calendar.getTime() );
			queryBuffer.append( "'" ).append( formattedStr ).append( "'" );
		}
		else {
			queryBuffer.append( dbKeyValue );
		}
		queryBuffer.append( " " );
	}

	public static String generatePrimaryKeyPredicate(EntityKey key) {
		StringBuilder buffer = new StringBuilder( 100 );
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			String columnName = key.getColumnNames()[i];
			if ( columnName.contains( "." ) ) {
				columnName = columnName.substring( columnName.indexOf( "." ) + 1 );
			}
			Object columnValue = key.getColumnValues()[i];
			buffer.append( columnName ).append( "=" );
			setFieldValue( buffer, columnValue );
			buffer.append( " and " );
		}
		buffer.setLength( buffer.length() - 5 );
		return buffer.toString();
	}

	public static boolean existsPrimaryKeyInDB(ODatabaseDocumentTx db, EntityKey key) {

		StringBuilder buffer = new StringBuilder( 100 );
		buffer.append( "select count(@rid) as c from " );
		buffer.append( key.getTable() ).append( " where " );
		buffer.append( generatePrimaryKeyPredicate( key ) );
		List<ODocument> documents = NativeQueryUtil.executeIdempotentQuery( db, buffer );
		Long count = (Long) ( documents.isEmpty() ? 0L : documents.get( 0 ).field( "c", Long.class ) );

		return ( count > 0 );
	}
}
