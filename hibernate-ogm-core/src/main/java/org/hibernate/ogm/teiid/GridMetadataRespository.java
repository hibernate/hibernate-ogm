/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.teiid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
import org.teiid.core.types.JDBCSQLTypeInfo;
import org.teiid.metadata.Column;
import org.teiid.metadata.Column.SearchType;
import org.teiid.metadata.KeyRecord;
import org.teiid.metadata.KeyRecord.Type;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.BaseMetadataRepository;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.TranslatorException;

public class GridMetadataRespository extends BaseMetadataRepository {
	private Iterator<org.hibernate.mapping.Table> tables;
	private OgmSessionFactory sf;
	
	public GridMetadataRespository(Iterator<org.hibernate.mapping.Table> tables, OgmSessionFactory sf) {
		this.tables = tables;
		this.sf = sf;
	}
	
	@Override
	public void loadMetadata(MetadataFactory factory, ExecutionFactory executionFactory, Object connectionFactory) throws TranslatorException {

		while(this.tables.hasNext()) {
			org.hibernate.mapping.Table tbl = this.tables.next();
			List<KeyRecord> uniqueKeys = new ArrayList<KeyRecord>();
			
			Table t = factory.addTable(tbl.getName());
			t.setVirtual(false);
			t.setSupportsUpdate(true);
			
			Iterator colIterator = tbl.getColumnIterator();
			while (colIterator.hasNext()) {
				org.hibernate.mapping.Column col = (org.hibernate.mapping.Column)colIterator.next();
				
				String type = "string"; //$NON-NLS-1$
				if (col.getSqlTypeCode() == null) {
					type = col.getSqlType(sf.getDialect(), sf);
				}
				
				Column c = factory.addColumn(col.getName(), JDBCSQLTypeInfo.getTypeName(JDBCSQLTypeInfo.getSQLType(type)), t);
				c.setUpdatable(true);				
				c.setPrecision(col.getPrecision());
				c.setLength(col.getLength());
				c.setScale(col.getScale());
				c.setSearchType(SearchType.All_Except_Like);
				
				if (col.isUnique()) {
					KeyRecord key = new KeyRecord(Type.Unique);
					uniqueKeys.add(key);
				}
			}

			// add PK
			ArrayList<String> pkNames = new ArrayList<String>();
			org.hibernate.mapping.PrimaryKey pk = tbl.getPrimaryKey();
			for (Object col:pk.getColumns()) {
				pkNames.add(((org.hibernate.mapping.Column)col).getName());
			}
			if (!pkNames.isEmpty()) {
				factory.addPrimaryKey(pk.getName(), pkNames, t);
			}
			
			// unique key
			if (!uniqueKeys.isEmpty()) {
				t.setUniqueKeys(uniqueKeys);
			}
			
			// Foreign Keys
			Iterator fkIterator = tbl.getForeignKeyIterator();
			while (fkIterator.hasNext()) {
				org.hibernate.mapping.ForeignKey fk = (org.hibernate.mapping.ForeignKey)fkIterator.next();
				ArrayList<String> fkNames = new ArrayList<String>();
				for (Object col:fk.getColumns()) {
					fkNames.add(((org.hibernate.mapping.Column)col).getName());
				}
				factory.addForiegnKey(fk.getName(), fkNames, fk.getReferencedTable().getName(), t);
			}
			
			// add index keys
			Iterator indexIterator = tbl.getIndexIterator();
			while(indexIterator.hasNext()) {
				org.hibernate.mapping.Index index = (org.hibernate.mapping.Index)indexIterator.next();
				ArrayList<String> idxNames = new ArrayList<String>();
				Iterator idxColIterator = index.getColumnIterator();
				while (idxColIterator.hasNext()) {
					org.hibernate.mapping.Column col = (org.hibernate.mapping.Column)idxColIterator.next();
					idxNames.add(col.getName());
				}
				factory.addIndex(index.getName(), false, idxNames, t);
			}
			
			// make Primary keys searchable
			for (Column column:t.getPrimaryKey().getColumns()) {
				column.setSearchType(SearchType.All_Except_Like);
			}
		}
		super.loadMetadata(factory, executionFactory, connectionFactory);
	}
}
