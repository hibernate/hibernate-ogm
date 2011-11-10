/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.spi.Tuple;

/**
 * Implements JDBC's ResultSet interface but is essentially a wrapper for
 * propagating a list of Map<String, Object> that reach represent a tuple.
 *
 * The ResultSet implementation is close to none (don't use it as a regular ResultSet)
 *  - currently only implement next() for moving along the set of tuples
 *  - implements unwrap / isWrapperFor for TupleAsMapResultSet.class
 *
 * Otherwise, to add a tuple, use addTuple.
 * To read the current tuple, use getTuple()
 * To move forward, use next() (throws a SQLException, I know that sucks)
 *
 * @author Emmanuel Bernard
 */
public class TupleAsMapResultSet implements ResultSet {
	private List<Tuple> tuples = new ArrayList<Tuple>();
	private int index = -1;

	/**
	 * Define the current value for the collection entry
	 */
	public void addTuple(Tuple tuple) {
		this.tuples.add( tuple );
	}

	/**
	 * Retrieve the current value for the collection entry
	 */
	public Tuple getTuple() {
		return tuples.get(index);
	}

	@Override
	public boolean next() throws SQLException {
		int currentIndex = index + 1;
		if ( currentIndex < tuples.size() ) {
			index = currentIndex;
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if ( iface == this.getClass() ) {
			return (T) this;
		}
		throw new SQLException("Cannot convert to " + iface);
	}

	@Override
	public void close() throws SQLException {
		tuples.clear();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return ( iface == this.getClass() ) ? true : false;
	}

	@Override
	public boolean wasNull() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void clearWarnings() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getCursorName() throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isFirst() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isLast() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void beforeFirst() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void afterLast() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean first() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean last() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getRow() throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean previous() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getFetchSize() throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getType() throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getConcurrency() throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean rowInserted() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void insertRow() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateRow() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void deleteRow() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void refreshRow() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void moveToInsertRow() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Statement getStatement() throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getHoldability() throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isClosed() throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
