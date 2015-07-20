package org.hibernate.ogm.datastore.elasticsearch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.annotations.VisibleForTesting;
import org.elasticsearch.common.base.Function;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.hibernate.ogm.datastore.elasticsearch.impl.ElasticSearchDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;

public class ElasticSearchDialect extends BaseGridDialect {

	private final ElasticSearchDatastoreProvider provider;

	private final Set<String> sequences = new HashSet<>();

	public ElasticSearchDialect(ElasticSearchDatastoreProvider provider) {
		super();
		this.provider = provider;
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		GetResponse getResponse = provider.getClient().prepareGet(provider.getDatabase(), key.getTable(), id(key)).setOperationThreaded(false).execute().actionGet();
		if (getResponse != null) {
			return new Tuple(new MapTupleSnapshot(getResponse.getSource()));
		}
		return null;
	}

	private String id(EntityKey key) {
		return String.valueOf(key.getColumnValues()[0]);
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Map<String, Object> map = map(key);
		return new Tuple(new MapTupleSnapshot(map));
	}

	private Map<String, Object> map(EntityKey key) {
		Map<String, Object> values = new HashMap<>();
		for (int i = 0; i < key.getColumnNames().length; i++) {
			values.put(key.getColumnNames()[i], key.getColumnValues()[i]);
		}
		return values;
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) throws TupleAlreadyExistsException {
		Map<String, Object> map = Maps.newHashMap(((MapTupleSnapshot) tuple.getSnapshot()).getMap());
		MapHelpers.applyTupleOpsOnMap(tuple, map);
		IndexRequest upsert = new IndexRequest(provider.getDatabase(), key.getTable(), id(key)).source(map).listenerThreaded(false);
		provider.getClient().prepareUpdate(provider.getDatabase(), key.getTable(), id(key)).setDoc(map).setUpsert(upsert).execute().actionGet();
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		provider.getClient().prepareDelete(provider.getDatabase(), key.getTable(), id(key)).setOperationThreaded(false).setRefresh(true).execute().actionGet();
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return null;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return null;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {

	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {

	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		String sequencesIndex = databaseSequencesIndex();
		if (!sequences.contains(request.getKey().getTable())) {
			final Map<String, Object> disabled = new HashMap<String, Object>() {
				{
					put("enabled", 0);
				}
			};
			final Map<String, Object> noIndex = new HashMap<String, Object>() {
				{
					put("index", "no");
				}
			};
			Map<String, Object> mapping = new HashMap<String, Object>() {
				{
					put("sequence", new HashMap<String, Object>() {
						{
							put("_source", disabled);
							put("_all", disabled);
							put("_type", noIndex);
							put("enabled", 0);
						}
					});
				}
			};
			provider.getClient().admin().indices().prepareCreate(sequencesIndex).setSettings(ImmutableSettings.settingsBuilder().put("number_of_shards", 1).put("auto_expand_replicas", "0-all"))
					.execute().actionGet();
			provider.getClient().admin().indices().preparePutMapping(sequencesIndex).setSource(mapping).execute().actionGet();
			sequences.add(request.getKey().getTable());
		}
		IndexResponse actionGet = provider.getClient().prepareIndex(sequencesIndex, request.getKey().getTable(), "sequence").execute().actionGet();
		return actionGet.getVersion();
	}

	private String databaseSequencesIndex() {
		return provider.getDatabase() + "_sequences";
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for (EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas) {
			List<Tuple> tuples = getTuples(entityKeyMetadata);
			for (Tuple tuple : tuples) {
				consumer.consume(tuple);
			}
		}
	}

	@Override
	protected List<Tuple> getTuples(EntityKeyMetadata entityKeyMetadata) {
		SearchResponse searchResponse = provider.getClient().prepareSearch(provider.getDatabase()).setTypes(entityKeyMetadata.getTable()).setSearchType(SearchType.QUERY_AND_FETCH)
				.addFields(entityKeyMetadata.getColumnNames()).execute().actionGet();
		if (searchResponse != null) {
			return Lists.transform(Arrays.asList(searchResponse.getHits().hits()), new Function<SearchHit, Tuple>() {
				@Override
				public Tuple apply(SearchHit hit) {
					return new Tuple(new MapTupleSnapshot(Maps.transformValues(hit.getFields(), new Function<SearchHitField, Object>() {
						@Override
						public Object apply(SearchHitField input) {
							return input.getValue();
						};
					})));
				}
			});
		}
		return null;
	}

	@VisibleForTesting
	protected void dropDatabase() {
		provider.getClient().prepareDeleteByQuery(provider.getDatabase()).setQuery(QueryBuilders.queryStringQuery("_all")).execute().actionGet();
		try {
			provider.getClient().prepareDeleteByQuery(databaseSequencesIndex()).setQuery(QueryBuilders.queryStringQuery("_all")).execute().actionGet();
		} catch (ElasticsearchException e) {
			// ignore
		}
	}
}