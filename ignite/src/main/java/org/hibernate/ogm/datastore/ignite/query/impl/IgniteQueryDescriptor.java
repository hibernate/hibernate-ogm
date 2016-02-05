package org.hibernate.ogm.datastore.ignite.query.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.ogm.dialect.query.spi.TypedGridValue;
import org.hibernate.type.Type;

public class IgniteQueryDescriptor implements Serializable {

	private static final long serialVersionUID = 8197979441369153954L;
	
	private final String originalSql;
	private final String sql;
	private final boolean hasScalar;
	private final List<Return> customQueryReturns;
	private final Set<String> querySpaces;
	
	public IgniteQueryDescriptor(String originalSql, String resultSql, SelectClause selectClause, Set<String> querySpaces){
		this.originalSql = originalSql;
		this.sql = resultSql;
		this.hasScalar = selectClause.isScalarSelect();
		List<Return> returnList = new ArrayList<>();
		List<String> columns = new ArrayList<String>();
		if (selectClause.getColumnNames() != null && selectClause.getColumnNames().length > 0) {
			for (int i = 0; i < selectClause.getColumnNames().length; i++) {
				columns.addAll(Arrays.asList(selectClause.getColumnNames()[i]));
			}
		}
		String[] aliases = columns.toArray(new String[columns.size()]);
		Type[] types = selectClause.getQueryReturnTypes();
		for (int i = 0; i < selectClause.getQueryReturnTypes().length; i++){
			returnList.add(new ScalarReturn(types[i], aliases != null ? aliases[i] : null));
		}
		this.customQueryReturns = Collections.unmodifiableList(returnList);
		this.querySpaces = querySpaces;
	}
	
	public IgniteQueryDescriptor(String originalSql, String sql){
		this.originalSql = originalSql;
		this.sql = sql;
		// SQL queries working only for scalars queries
		this.hasScalar = true;
		this.customQueryReturns = null;
		this.querySpaces = Collections.EMPTY_SET;
	}
	
	public String getOriginalSql() {
		return originalSql;
	}

	public String getSql() {
		return sql;
	}

	public boolean isHasScalar() {
		return hasScalar;
	}

	public List<Return> getCustomQueryReturns() {
		return customQueryReturns;
	}

	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	public static List<Object> createParameterList(String originSql, Map<String, TypedGridValue> parameterMap){
		List<Object> result = new ArrayList<>();
		int pos = 0;
		String subStr = originSql;
		Pattern pattern = Pattern.compile(".*?(:\\w+).*");
		Matcher matcher = pattern.matcher(subStr);
		while (matcher.matches()){
			String param = matcher.group(1);
			result.add(parameterMap.get(param.substring(1)).getValue());
			pos = subStr.indexOf(param) + param.length();
			subStr = subStr.substring(pos);
			matcher = pattern.matcher(subStr);
		}
				
		return result;
	}
	
}
