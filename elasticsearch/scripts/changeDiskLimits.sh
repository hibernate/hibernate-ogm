	curl -XPUT localhost:9200/_cluster/settings -d '{
	    "transient" : {
	        "cluster.routing.allocation.disk.watermark.low" : "92%",
	        "cluster.routing.allocation.disk.watermark.high" : "95%"
	    }
	}'