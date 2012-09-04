package org.hibernate.ogm.teiid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.teiid.metadata.AbstractMetadataRecord;
import org.teiid.metadata.Table;

public class GridUtil {

	public static EntityKey buildEntityKey(Table t, HashMap<String, Object> columnValues) {
		List<String> pkColumns = getNames(t.getPrimaryKey().getColumns());
		ArrayList<Object> values = new ArrayList<Object>();
		for (String col:pkColumns) {
			values.add(columnValues.get(col));
		}
		return new EntityKey(t.getName(), pkColumns.toArray(new String[pkColumns.size()]), values.toArray());
	}
	
	public static RowKey buildRowKey(Table t, HashMap<String, Object> columnValues) {
		List<String> pkColumns = getNames(t.getPrimaryKey().getColumns());
		ArrayList<Object> values = new ArrayList<Object>();
		for (String col:pkColumns) {
			values.add(columnValues.get(col));
		}
		return new RowKey(t.getName(), pkColumns.toArray(new String[pkColumns.size()]), values.toArray());
	}	
	
	public static List<String> getNames (List<? extends AbstractMetadataRecord> records){
		ArrayList<String> names = new ArrayList<String>();
		for (AbstractMetadataRecord record:records) {
			names.add(record.getCanonicalName());
		}
		return names;
	}	
}
