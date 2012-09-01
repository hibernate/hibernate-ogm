package org.hibernate.ogm.teiid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.impl.GridDialectFactory;
import org.teiid.core.types.DataTypeManager;
import org.teiid.core.types.JDBCSQLTypeInfo;
import org.teiid.metadata.AbstractMetadataRecord;
import org.teiid.metadata.Column;
import org.teiid.metadata.Column.SearchType;
import org.teiid.metadata.ColumnStats;
import org.teiid.metadata.KeyRecord;
import org.teiid.metadata.KeyRecord.Type;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.MetadataRepository;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.Table;
import org.teiid.metadata.Table.TriggerEvent;
import org.teiid.metadata.TableStats;
import org.teiid.query.metadata.BaseMetadataRepository;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.TranslatorException;

public class GridMetadataRespository extends BaseMetadataRepository {
	private Iterator<org.hibernate.mapping.Table> tables;
	private SessionFactoryImpl sf;
	
	public GridMetadataRespository(Iterator<org.hibernate.mapping.Table> tables, SessionFactoryImpl sf) {
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
				
				String type = "string";
				if (col.getSqlTypeCode() == null) {
					type = col.getSqlType(sf.getDialect(), sf);
				}
				
				Column c = factory.addColumn(col.getName(), JDBCSQLTypeInfo.getTypeName(JDBCSQLTypeInfo.getSQLType(type)), t);
				c.setUpdatable(true);				
				c.setPrecision(col.getPrecision());
				c.setLength(col.getLength());
				c.setScale(col.getScale());
				c.setSearchType(SearchType.Unsearchable);
				
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
					idxNames.add(((org.hibernate.mapping.Column)col).getName());
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
